package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.Product
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ProductRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing inventory.")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user inventory access." }
        }
        return uid
    }

    private fun productsCollection(uid: String) =
        db.collection("users").document(uid).collection("products")

    fun observeProducts(userId: String): Flow<List<Product>> = flow {
        val uid = requireUserId(userId)
        val colRef = productsCollection(uid)
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Product::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.featured }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun getProducts(userId: String): Result<List<Product>> {
        val path = "users/$userId/products"
        return try {
            val uid = requireUserId(userId)
            val snapshot = productsCollection(uid).get().await()
            val products = snapshot.toObjects(
                Product::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.featured }
            Result.success(products)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getProductById(userId: String, productId: String): Result<Product?> {
        val path = "users/$userId/products/$productId"
        return try {
            val uid = requireUserId(userId)
            require(productId.isNotBlank()) { "Product ID must not be blank." }
            val snap = productsCollection(uid).document(productId).get().await()
            val product = snap.toObject(
                Product::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(product)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun createProduct(
        userId: String,
        product: Product,
        customDocId: String? = null
    ): Result<Product> {
        val path = "users/$userId/products/${customDocId ?: "new"}"
        return try {
            val uid = requireUserId(userId)
            validateProductForRules(product)
            val normalized = product.normalized(ownerUid = uid)
            val colRef = productsCollection(uid)
            val docRef = if (customDocId.isNullOrBlank()) {
                colRef.document()
            } else {
                colRef.document(customDocId)
            }
            docRef.set(normalized.toCreateMap()).await()
            Result.success(normalized.copy(id = docRef.id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProduct(userId: String, product: Product): Result<Product> {
        val path = "users/$userId/products/${product.id}"
        return try {
            val uid = requireUserId(userId)
            require(product.id.isNotBlank()) { "Product ID must not be blank for update." }
            validateProductForRules(product)
            val normalized = product.normalized(ownerUid = uid)
            productsCollection(uid)
                .document(product.id)
                .update(normalized.toUpdateMap())
                .await()
            Result.success(normalized)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun adjustStock(
        userId: String,
        product: Product,
        newStockCount: Long
    ): Result<Long> {
        val path = "users/$userId/products/${product.id}"
        return try {
            val uid = requireUserId(userId)
            require(product.id.isNotBlank()) { "Product ID must not be blank." }
            val clampedStock = newStockCount.coerceIn(0L, 100000L)
            productsCollection(uid)
                .document(product.id)
                .update(
                    mapOf(
                        "stockCount" to clampedStock,
                        "soldCount" to product.soldCount.coerceIn(0L, 1000000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(clampedStock)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(userId: String, productId: String): Result<Unit> {
        val path = "users/$userId/products/$productId"
        return try {
            val uid = requireUserId(userId)
            require(productId.isNotBlank()) { "Product ID must not be blank." }
            productsCollection(uid)
                .document(productId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun validateProductForRules(product: Product) {
        val cleanName = product.name.trim()
        require(cleanName.length in 2..80) { "Product name must be between 2 and 80 characters." }
        require(product.price > 0.0 && product.price <= 10000.0) {
            "Price must be between $0.01 and $10,000."
        }
        require(product.costPrice >= 0.0 && product.costPrice <= 10000.0) {
            "Cost price must be between $0 and $10,000."
        }
        require(product.stockCount in 0L..100000L) {
            "Stock count must be between 0 and 100,000."
        }
    }
}

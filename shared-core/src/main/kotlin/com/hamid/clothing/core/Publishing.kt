package com.hamid.clothing.core

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Public shop details that customers see. */
data class CatalogStoreInfo(
    val shopName: String = "",
    val phone: String = "",
    val address: String = "",
    val whatsapp: String = "",
    val telegram: String = "",
    val instagram: String = "",
    val rubika: String = "",
    val eitaa: String = "",
    val bale: String = "",
    val courierCost: Long = 0L,
    val postCost: Long = 0L
)

/** Everything the customer app needs to show the shop. Contains no costs and no private data. */
data class CatalogData(
    val publishedAt: Long,
    val store: CatalogStoreInfo,
    val categories: List<Category>,
    val products: List<Product>,
    val discounts: List<Discount> = emptyList(),
    val bankAccounts: List<BankAccount> = emptyList()
)

object CatalogBuilder {
    /**
     * Builds the public catalogue from the shop data:
     * only active products, without purchase or landed cost, only categories in use, only bank accounts
     * the owner chose to show, and only image names that are safe file names.
     */
    fun fromSnapshot(s: StoreSnapshot, now: Long, discounts: List<Discount> = emptyList()): CatalogData {
        val products = s.products
            .filter { it.active }
            .map { p ->
                p.copy(
                    purchasePrice = 0L,
                    landedCost = 0L,
                    imageUris = p.imageUris.filter { CatalogPackage.isSafeImageName(it) }
                )
            }
        val usedCategories = products.map { it.categoryId }.toSet()
        val st = s.settings
        return CatalogData(
            publishedAt = now,
            store = CatalogStoreInfo(
                st.shopName, st.phone, st.address, st.whatsapp, st.telegram, st.instagram,
                st.rubika, st.eitaa, st.bale, st.courierCost, st.postCost
            ),
            categories = s.categories.filter { it.id in usedCategories },
            products = products,
            discounts = discounts,
            bankAccounts = s.bankAccounts.filter { it.active && it.visibleToCustomers }
        )
    }

    /** Products a customer would see as unfinished: no photo, no description or nothing in stock. */
    fun warnings(s: StoreSnapshot): List<String> {
        val out = mutableListOf<String>()
        for (p in s.products.filter { it.active }) {
            if (p.imageUris.isEmpty()) out.add("«${p.name}» عکس ندارد")
            if (p.description.isBlank()) out.add("«${p.name}» توضیحات ندارد")
            if (p.totalStock <= 0) out.add("«${p.name}» موجودی ندارد")
        }
        return out
    }
}

/** The file that carries the catalogue: catalog.json plus images/<name>. */
object CatalogPackage {
    const val JSON_ENTRY = "catalog.json"
    const val IMAGE_DIR = "images/"
    const val MAX_ENTRY_BYTES = 8L * 1024 * 1024
    const val MAX_TOTAL_BYTES = 300L * 1024 * 1024

    private val SAFE_NAME = Regex("^[A-Za-z0-9._-]{1,80}$")

    /** Letters, digits, dot, dash, underscore only; never a path. */
    fun isSafeImageName(name: String): Boolean = SAFE_NAME.matches(name) && !name.contains("..")

    /** Writes the package. Images are loaded one by one so memory stays small. Returns how many images were stored. */
    fun write(out: OutputStream, data: CatalogData, loadImage: (String) -> ByteArray?): Int {
        val zip = ZipOutputStream(out)
        zip.putNextEntry(ZipEntry(JSON_ENTRY))
        zip.write(DataCodec.encodeCatalog(data).toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        var count = 0
        for (name in data.products.flatMap { it.imageUris }.distinct()) {
            if (!isSafeImageName(name)) continue
            val bytes = loadImage(name) ?: continue
            zip.putNextEntry(ZipEntry(IMAGE_DIR + name))
            zip.write(bytes)
            zip.closeEntry()
            count++
        }
        zip.finish()
        return count
    }

    /**
     * Reads a package. Unsafe entry names are ignored (no path tricks), every entry and the total are size-limited.
     * [onImage] receives (file name, bytes) for each image.
     */
    fun read(input: InputStream, onImage: (String, ByteArray) -> Unit): CatalogData {
        val zip = ZipInputStream(input)
        var data: CatalogData? = null
        var total = 0L
        while (true) {
            val entry = zip.nextEntry ?: break
            if (entry.isDirectory) continue
            val bytes = readLimited(zip)
            total += bytes.size
            require(total <= MAX_TOTAL_BYTES) { "catalog file is too large" }
            val name = entry.name
            if (name == JSON_ENTRY) {
                data = DataCodec.decodeCatalog(String(bytes, Charsets.UTF_8))
            } else if (name.startsWith(IMAGE_DIR)) {
                val file = name.substring(IMAGE_DIR.length)
                if (isSafeImageName(file)) onImage(file, bytes)
            }
        }
        return data ?: throw IllegalArgumentException("catalog.json not found")
    }

    private fun readLimited(zip: ZipInputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var size = 0L
        while (true) {
            val n = zip.read(buffer)
            if (n < 0) break
            size += n
            require(size <= MAX_ENTRY_BYTES) { "catalog entry is too large" }
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
}

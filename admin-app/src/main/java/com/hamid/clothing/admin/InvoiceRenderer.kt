package com.hamid.clothing.admin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import com.hamid.clothing.core.InvoiceModel
import java.io.File

/** Draws an [InvoiceModel] as a picture (PNG) that can be sent through any messenger. */
object InvoiceRenderer {
    private const val WIDTH = 1080
    private const val MARGIN = 48
    private const val CONTENT = WIDTH - 2 * MARGIN

    private const val INK = 0xFF1E1A14.toInt()
    private const val MUTED = 0xFF6D655A.toInt()
    private const val GOLD = 0xFFA97A1C.toInt()
    private const val LINE = 0xFFDDD3C3.toInt()
    private const val HEADER_BG = 0xFFF3EBDD.toInt()
    private const val WHITE = 0xFFFFFFFF.toInt()

    private class Block(val height: Int, val draw: (Canvas, Int) -> Unit)

    private fun paint(size: Float, color: Int, bold: Boolean = false): TextPaint {
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG)
        p.textSize = size
        p.color = color
        p.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        return p
    }

    private fun layout(text: String, paint: TextPaint, width: Int, align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(align)
            .setTextDirection(TextDirectionHeuristics.RTL)
            .setIncludePad(false)
            .build()

    private fun drawLayout(canvas: Canvas, l: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        l.draw(canvas)
        canvas.restore()
    }

    private fun line(color: Int = LINE): Paint = Paint().also { it.color = color; it.strokeWidth = 2f }

    fun render(model: InvoiceModel): Bitmap {
        val blocks = mutableListOf<Block>()
        val body = paint(30f, INK)
        val bodyBold = paint(30f, INK, bold = true)
        val small = paint(26f, MUTED)
        val head = paint(32f, INK, bold = true)

        // ---- shop name and invoice number
        run {
            val rightW = (CONTENT * 0.62f).toInt()
            val leftW = CONTENT - rightW - 24
            val name = layout(model.shopName, paint(56f, INK, bold = true), rightW)
            val shopLines = model.shopLines.map { layout(it, small, rightW) }
            val number = layout("شماره: " + model.number, bodyBold, leftW)
            val date = layout("تاریخ: " + model.date, body, leftW)
            val rightH = name.height + 8 + shopLines.sumOf { it.height + 4 }
            val leftH = number.height + 8 + date.height
            blocks.add(Block(maxOf(rightH, leftH) + 24) { c, top ->
                var y = top.toFloat()
                drawLayout(c, name, (WIDTH - MARGIN - rightW).toFloat(), y)
                y += name.height + 8
                for (l in shopLines) {
                    drawLayout(c, l, (WIDTH - MARGIN - rightW).toFloat(), y)
                    y += l.height + 4
                }
                drawLayout(c, number, MARGIN.toFloat(), top.toFloat())
                drawLayout(c, date, MARGIN.toFloat(), top + number.height + 8f)
            })
        }

        // ---- title bar
        run {
            val title = layout(model.title, paint(40f, WHITE, bold = true), CONTENT, Layout.Alignment.ALIGN_CENTER)
            blocks.add(Block(title.height + 36) { c, top ->
                val bg = Paint(Paint.ANTI_ALIAS_FLAG).also { it.color = GOLD }
                c.drawRoundRect(RectF(MARGIN.toFloat(), top.toFloat(), (WIDTH - MARGIN).toFloat(), top + title.height + 28f), 18f, 18f, bg)
                drawLayout(c, title, MARGIN.toFloat(), top + 14f)
            })
        }

        // ---- customer and delivery details
        run {
            val parts = mutableListOf<StaticLayout>()
            parts.add(layout("مشتری: " + model.customerName, head, CONTENT))
            for ((label, value) in model.details) parts.add(layout("$label: $value", body, CONTENT))
            val h = parts.sumOf { it.height + 6 }
            blocks.add(Block(h + 24) { c, top ->
                var y = top + 12f
                for (l in parts) {
                    drawLayout(c, l, MARGIN.toFloat(), y)
                    y += l.height + 6
                }
            })
        }

        // ---- items table
        val weights = floatArrayOf(0.07f, 0.36f, 0.17f, 0.09f, 0.15f, 0.16f) // right to left
        val widths = weights.map { (CONTENT * it).toInt() }
        val pad = 10
        fun rowBlock(cells: List<String>, painter: TextPaint, shaded: Boolean): Block {
            val layouts = cells.mapIndexed { i, text ->
                val align = if (i == 1 || i == 2) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_CENTER
                layout(text, painter, widths[i] - 2 * pad, align)
            }
            val h = (layouts.maxOf { it.height }) + 2 * 14
            return Block(h) { c, top ->
                if (shaded) {
                    val bg = Paint().also { it.color = HEADER_BG }
                    c.drawRect(MARGIN.toFloat(), top.toFloat(), (WIDTH - MARGIN).toFloat(), top + h.toFloat(), bg)
                }
                var right = (WIDTH - MARGIN).toFloat()
                for (i in layouts.indices) {
                    val left = right - widths[i]
                    drawLayout(c, layouts[i], left + pad, top + 14f)
                    right = left
                }
                c.drawLine(MARGIN.toFloat(), top + h.toFloat(), (WIDTH - MARGIN).toFloat(), top + h.toFloat(), line())
            }
        }
        blocks.add(rowBlock(listOf("ردیف", "نام کالا", "رنگ / سایز", "تعداد", "قیمت واحد", "مبلغ"), head, shaded = true))
        for (r in model.rows) {
            blocks.add(rowBlock(listOf(r.index, r.name, r.variant, r.quantity, r.unitPrice, r.total), body, shaded = false))
        }

        // ---- totals
        run {
            val rows = model.totals.mapIndexed { i, (label, value) ->
                val strong = label.startsWith("مبلغ کل")
                val p = if (strong) paint(34f, INK, bold = true) else body
                val l = layout(label, p, CONTENT / 2)
                val v = layout(value, p, CONTENT / 2 - 16, Layout.Alignment.ALIGN_OPPOSITE)
                Triple(l, v, strong || i < 0)
            }
            val h = rows.sumOf { maxOf(it.first.height, it.second.height) + 14 }
            blocks.add(Block(h + 24) { c, top ->
                var y = top + 12f
                for ((l, v, strong) in rows) {
                    val rowH = maxOf(l.height, v.height)
                    if (strong) {
                        val bg = Paint().also { it.color = HEADER_BG }
                        c.drawRect(MARGIN.toFloat(), y - 6, (WIDTH - MARGIN).toFloat(), y + rowH + 8, bg)
                    }
                    drawLayout(c, l, (WIDTH - MARGIN - CONTENT / 2).toFloat() + 16, y)
                    drawLayout(c, v, MARGIN.toFloat() + 8, y)
                    y += rowH + 14
                }
            })
        }

        // ---- footer
        run {
            val footer = layout(model.footer, paint(30f, GOLD, bold = true), CONTENT, Layout.Alignment.ALIGN_CENTER)
            val shop = layout(model.shopName, small, CONTENT, Layout.Alignment.ALIGN_CENTER)
            blocks.add(Block(footer.height + shop.height + 56) { c, top ->
                c.drawLine(MARGIN.toFloat(), top + 8f, (WIDTH - MARGIN).toFloat(), top + 8f, line(GOLD))
                drawLayout(c, footer, MARGIN.toFloat(), top + 28f)
                drawLayout(c, shop, MARGIN.toFloat(), top + 36f + footer.height)
            })
        }

        val top = 30
        val total = top + blocks.sumOf { it.height } + 30
        val bitmap = Bitmap.createBitmap(WIDTH, total, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(WHITE)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), 14f, Paint().also { it.color = GOLD })
        var y = top
        for (b in blocks) {
            b.draw(canvas, y)
            y += b.height
        }
        return bitmap
    }

    /** Renders and saves the picture in the cache folder shared through the file provider. */
    fun renderToFile(context: Context, model: InvoiceModel): File {
        val dir = File(context.cacheDir, "invoices")
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        val safe = model.number.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(dir, "invoice-$safe.png")
        val bitmap = render(model)
        try {
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            bitmap.recycle()
        }
        return file
    }
}

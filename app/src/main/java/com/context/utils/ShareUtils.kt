package com.context.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import com.context.app.R
import com.context.data.Expense
import com.context.data.Group
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareUtils {

    fun shareGroupSummary(context: Context, group: Group, expenses: List<Expense>) {
        val total = expenses.filter { it.category != "Settlement" }.sumOf { it.amount }
        
        // Build the text message for the share sheet
        val sb = StringBuilder()
        sb.append("Hey! Here is the split for the ${group.name}. Total: ₹${String.format("%.2f", total)}.\n\n")
        sb.append("Calculated with Cleave")

        // Generate the professional receipt image
        val bitmap = createReceiptBitmap(context, group, total, expenses)
        val imageUri = saveBitmapToCache(context, bitmap)

        // Launch Android Share Sheet
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            if (imageUri != null) {
                putExtra(Intent.EXTRA_STREAM, imageUri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }

        val shareIntent = Intent.createChooser(sendIntent, "Share Receipt via...")
        context.startActivity(shareIntent)
    }

    private fun createReceiptBitmap(context: Context, group: Group, total: Double, expenses: List<Expense>): Bitmap {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.layout_receipt_share, null)

        // Bind Data
        view.findViewById<TextView>(R.id.tv_group_name).text = "Group: ${group.name}"
        view.findViewById<TextView>(R.id.tv_date).text = "Date: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())}"
        view.findViewById<TextView>(R.id.tv_total_amount).text = "₹${String.format("%.2f", total)}"

        val membersContainer = view.findViewById<LinearLayout>(R.id.members_container)
        val members = group.getMemberList()
        val perPerson = if (members.isNotEmpty()) total / members.size else total

        members.forEach { member ->
            val memberView = inflater.inflate(R.layout.item_receipt_member, membersContainer, false)
            memberView.findViewById<TextView>(R.id.tv_member_name).text = member
            memberView.findViewById<TextView>(R.id.tv_member_amount).text = "₹${String.format("%.2f", perPerson)}"
            membersContainer.addView(memberView)
        }

        // Measure and Layout the view
        view.measure(
            View.MeasureSpec.makeMeasureSpec(1050, View.MeasureSpec.EXACTLY), // 350dp * 3
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        // Draw to Bitmap
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        val imagesFolder = File(context.cacheDir, "images")
        imagesFolder.mkdirs()
        val file = File(imagesFolder, "cleave_receipt_${System.currentTimeMillis()}.png")
        
        return try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            FileProvider.getUriForFile(context, "com.context.app.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

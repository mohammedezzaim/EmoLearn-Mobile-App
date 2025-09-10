package com.example.gencont_app.cours

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.example.gencont_app.R
import com.example.gencont_app.configDB.sqlite.data.*

class ChapiterAdapter(
    private val context: Context,
    private val sections: List<Section>
) : BaseAdapter() {

    override fun getCount(): Int = sections.size

    override fun getItem(position: Int): Any = sections[position]

    override fun getItemId(position: Int): Long = sections[position].id

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_section, parent, false)

        val section = sections[position]

        val tvTitle = view.findViewById<TextView>(R.id.tvSectionTitle)
        val tvDescription = view.findViewById<TextView>(R.id.tvSectionOrder)

        tvTitle.text = "${section.numeroOrder}. ${section.titre}"
        val descText = section.contenu

        tvDescription.text = if (descText.length > 55)
            descText.take(55) + "..."
        else
            descText


//        tvDescription.text = section.contenu

        val btnVoirContenu = view.findViewById<Button>(R.id.btnVoirContenu)
        btnVoirContenu.setOnClickListener {
            val intent = Intent(context, CourSectionActivity::class.java)
            intent.putExtra("section_id", section.id)
            context.startActivity(intent)
        }
//        view.setOnClickListener {
//            val intent = Intent(context, CourSectionActivity::class.java)
//            intent.putExtra("section_id", section.id)
//            context.startActivity(intent)
//        }


        return view
    }
}
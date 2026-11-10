package com.aaditya.mediashrinker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PatchNotesActivity : AppCompatActivity() {

    data class Change(val tag: String, val description: String)
    data class PatchNote(val version: String, val date: String, val title: String, val isLatest: Boolean, val changes: List<Change>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_patch_notes)

        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }

        val recyclerView = findViewById<RecyclerView>(R.id.patchNotesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = PatchNoteAdapter(buildPatchNotes())

        // Staggered slide-up + fade entrance for each version card.
        recyclerView.layoutAnimation = AnimationUtils.loadLayoutAnimation(this, R.anim.patch_notes_layout_anim)
        recyclerView.scheduleLayoutAnimation()
    }

    private fun buildPatchNotes(): List<PatchNote> = listOf(
        PatchNote(
            version = "v11.1",
            date = "12 Sep 2026",
            title = "In-app camera, real photo deletion & bigger limits",
            isLatest = true,
            changes = listOf(
                Change("NEW", "Built a fully in-app camera — the system camera app is never opened anymore. Choose Single Photo or Multiple Photos mode, review shots in a thumbnail strip, and confirm with one tap."),
                Change("NEW", "Added Patch Notes and Terms of Use to Settings."),
                Change("REMOVED", "Removed the old \"max 3 times\" cap on camera captures — click as many photos as you need."),
                Change("FIXED", "Magic Cleaner's Delete button now actually deletes photos through MediaStore instead of just showing a toast — deleted photos disappear from your gallery immediately too."),
                Change("FIXED", "Fixed the delete permission dialog popping up separately for every single photo — batch deletes on Android 11+ now show one confirmation for the whole selection, and Android 10 and below no longer prompt per photo at all."),
                Change("FIXED", "Fixed a duplicate-detection bug in Magic Cleaner where a calculation error (channel values were combined before masking) made the similarity hash unreliable — duplicate matching is noticeably more accurate now."),
                Change("IMPROVED", "Magic Cleaner: added a one-tap \"Auto-select duplicates\" action that keeps the newest copy in each group and selects the rest, plus a confirmation dialog before deleting."),
                Change("IMPROVED", "Metadata Remover rebuilt: select multiple images at once, see exactly which metadata (GPS, camera model, timestamps, etc.) was found before removing it, and metadata is now stripped losslessly — no more forced JPEG conversion that used to destroy PNG transparency and image quality."),
                Change("IMPROVED", "Raised the one-time PDF photo limit from 50 to 1000."),
                Change("IMPROVED", "Raised the compression photo-selection limit from 100 to 200, and the total size cap from 2GB to 5GB."),
                Change("IMPROVED", "Reorganized the home screen so all core actions fit without scrolling, with cleaner spacing and more consistent typography.")
            )
        ),
        PatchNote(
            version = "v11.0",
            date = "—",
            title = "Prior stable release",
            isLatest = false,
            changes = listOf(
                Change("NEW", "Baseline feature set: compression modes, PDF creation, Magic Cleaner, Metadata Remover, Big File Hunter, Storage Analytics, and more.")
            )
        )
    )

    inner class PatchNoteAdapter(private val notes: List<PatchNote>) : RecyclerView.Adapter<PatchNoteAdapter.VH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_patch_note, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val note = notes[position]
            holder.versionBadge.text = note.version
            holder.dateText.text = note.date
            holder.titleText.text = note.title
            holder.latestPill.visibility = if (note.isLatest) View.VISIBLE else View.GONE

            holder.changesContainer.removeAllViews()
            note.changes.forEach { change ->
                val row = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.item_patch_note_change, holder.changesContainer, false)
                val tagText = row.findViewById<TextView>(R.id.changeTagText)
                val descText = row.findViewById<TextView>(R.id.changeDescText)
                tagText.text = change.tag
                descText.text = change.description
                tagText.background = when (change.tag) {
                    "FIXED" -> resources.getDrawable(R.drawable.tag_fixed_bg)
                    "IMPROVED" -> resources.getDrawable(R.drawable.tag_improved_bg)
                    else -> resources.getDrawable(R.drawable.tag_new_bg)
                }
                holder.changesContainer.addView(row)
            }
        }

        override fun getItemCount() = notes.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val versionBadge: TextView = v.findViewById(R.id.patchVersionBadge)
            val latestPill: TextView = v.findViewById(R.id.patchLatestPill)
            val dateText: TextView = v.findViewById(R.id.patchDateText)
            val titleText: TextView = v.findViewById(R.id.patchTitleText)
            val changesContainer: LinearLayout = v.findViewById(R.id.patchChangesContainer)
        }
    }
}

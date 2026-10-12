package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NoteImagesTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun addRemoveAndRestoreKeepOrder() {
        val s = NoteStore(); val n = s.add("a")
        s.addImage(n.id, "one.jpg"); s.addImage(n.id, "two.jpg"); s.addImage(n.id, "two.jpg")
        assertEquals(listOf("one.jpg", "two.jpg"), s.all().single().images)
        s.removeImage(n.id, "one.jpg")
        assertEquals(listOf("two.jpg"), s.all().single().images)
        s.addImage(n.id, "one.jpg", index = 0)
        assertEquals(listOf("one.jpg", "two.jpg"), s.all().single().images)
    }

    @Test fun unsafeNamesAreRejected() {
        val s = NoteStore(); val n = s.add("a")
        for (bad in listOf("../x.jpg", "a/b.jpg", "", ".hidden", "a:b.jpg", "a,b.jpg")) {
            assertFalse(bad, ImageName.isSafe(bad))
            s.addImage(n.id, bad)
        }
        assertTrue(s.all().single().images.isEmpty())
        assertTrue(ImageName.isSafe("3f2a-b.JPG"))
    }

    @Test fun serializationRoundTripsAndOldDataMigrates() {
        val s = NoteStore(); val n = s.add("hello", now = 5); s.setReminder(n.id, 99); s.addImage(n.id, "a.jpg"); s.addImage(n.id, "b.jpg")
        val back = NoteStore.deserialize(s.serialize()).all().single()
        assertEquals(listOf("a.jpg", "b.jpg"), back.images)
        assertEquals(99L, back.remindAt)
        // Data written before images existed (9 meta fields) still loads, with no images.
        val old = NoteStore.deserialize("1\tfalse:5:false::::::7\told note").all().single()
        assertEquals(emptyList<String>(), old.images)
        assertEquals(7L, old.remindAt)
        assertEquals("old note", old.text)
    }

    @Test fun purgeCascadeOrphansOnlyUnreferencedFiles() {
        val s = NoteStore()
        val a = s.add("a"); val b = s.add("b")
        s.addImage(a.id, "a.jpg"); s.addImage(b.id, "shared.jpg"); s.addImage(a.id, "shared.jpg")
        val onDisk = listOf("a.jpg", "shared.jpg", "stray.jpg")
        assertEquals(listOf("stray.jpg"), s.orphanedImages(onDisk))
        s.delete(a.id)
        assertEquals(listOf("stray.jpg"), s.orphanedImages(onDisk)) // still in Trash, so kept for Restore
        s.emptyTrash()
        assertEquals(listOf("a.jpg", "stray.jpg"), s.orphanedImages(onDisk))
    }

    @Test fun expiredTrashReleasesImages() {
        val s = NoteStore(); val a = s.add("a"); s.addImage(a.id, "a.jpg")
        s.delete(a.id, now = 0)
        s.purgeExpired(now = NoteStore.TRASH_RETENTION_MS + 1)
        assertEquals(listOf("a.jpg"), s.orphanedImages(listOf("a.jpg")))
    }

    @Test fun duplicateSharesImages() {
        val s = NoteStore(); val a = s.add("a"); s.addImage(a.id, "a.jpg")
        assertEquals(listOf("a.jpg"), s.duplicate(a.id)!!.images)
    }

    @Test fun imageFilesSweepDeletesOnlyOrphans() {
        val files = ImageFiles(tmp.newFolder("images"))
        val keep = files.newName("jpg"); val drop = files.newName("jpg")
        files.file(keep).writeBytes(byteArrayOf(1)); files.file(drop).writeBytes(byteArrayOf(2))
        val s = NoteStore(); val n = s.add("a"); s.addImage(n.id, keep)
        assertEquals(setOf(keep, drop), files.names().toSet())
        files.sweep(s)
        assertEquals(listOf(keep), files.names())
        assertTrue(ImageName.isSafe(keep))
        assertNull(files.fileOrNull("../evil"))
    }

    @Test fun backupIncludesAndRestoresImages() {
        val s = NoteStore(); val n = s.add("pic", now = 1); s.addImage(n.id, "p.jpg")
        val json = Backup.export(s.all()) { name -> if (name == "p.jpg") byteArrayOf(1, 2, 3) else null }
        val written = mutableMapOf<String, ByteArray>()
        val restored = NoteStore()
        val r = Backup.import(restored, json) { name, bytes -> written[name] = bytes } as Backup.Result.Imported
        assertEquals(1, r.added)
        assertEquals(listOf("p.jpg"), restored.all().single().images)
        assertEquals(listOf<Byte>(1, 2, 3), written["p.jpg"]!!.toList())
        // Merging the same backup again writes nothing.
        written.clear()
        Backup.import(restored, json) { name, bytes -> written[name] = bytes }
        assertTrue(written.isEmpty())
    }

    @Test fun backupWithoutImagesStillImportsAndTraversalIsRejected() {
        assertEquals(1, (Backup.import(NoteStore(), "{\"version\":1,\"notes\":[{\"text\":\"x\"}]}") as Backup.Result.Imported).added)
        val evil = "{\"version\":1,\"notes\":[{\"text\":\"x\",\"images\":[\"../../x.jpg\"]}],\"files\":{\"../../x.jpg\":\"AQ==\"}}"
        val s = NoteStore()
        assertTrue(Backup.import(s, evil) { _, _ -> error("must not write") } is Backup.Result.Error)
        assertTrue(s.all().isEmpty())
    }

    @Test fun downscaleKeepsAspectAndNeverUpscales() {
        assertEquals(ImageScale.Size(2048, 1024), ImageScale.fit(4096, 2048))
        assertEquals(ImageScale.Size(1024, 2048), ImageScale.fit(2000, 4000))
        assertEquals(ImageScale.Size(800, 600), ImageScale.fit(800, 600))
        assertEquals(ImageScale.Size(1, 2048), ImageScale.fit(1, 99999))
        assertEquals(4, ImageScale.sampleSize(4096, 4096, 1024))
        assertEquals(1, ImageScale.sampleSize(500, 500, 1024))
    }

    @Test fun cardDescriptionMentionsPhotos() {
        val labels = NoteAccessibility.Labels("Pinned", { "Colour $it" }, { "Tags $it" }, photos = { "$it photos" })
        assertEquals("Hi. 2 photos", NoteAccessibility.describe("Hi", false, null, emptyList(), labels, imageCount = 2))
        assertEquals("Hi", NoteAccessibility.describe("Hi", false, null, emptyList(), labels))
    }
}

package com.conght.facemakerapp.core.ui.skeleton

import com.conght.facemakerapp.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** Mọi lưới phụ kiện: ô None luôn ở vị trí 0 (cả lúc shimmer lẫn khi có dữ liệu). */
class NoneFirstTest {

    @Test
    fun noneIsFirst_whileLoadingAndWithItems() {
        val none = NoneAdapter(R.layout.item_none_part) {}
        val dice = NoneAdapter(R.layout.item_dice_part) {}
        val skeleton = SkeletonAdapter(R.layout.item_skeleton_part, 18)

        val loading = noneFirstAdapters(none, dice, skeleton)
        assertSame(none, loading.first())
        assertEquals(listOf(none, dice, skeleton), loading)
        assertEquals(1, none.itemCount)

        val decor = noneFirstAdapters(none, SkeletonAdapter(R.layout.item_skeleton_decor, 3))
        assertSame(none, decor.first())
    }
}

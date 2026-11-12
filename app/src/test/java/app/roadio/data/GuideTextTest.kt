package app.roadio.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GuideTextTest {
    @Test
    fun `방향과 이름 앞머리를 뗀다`() {
        assertEquals(
            "해발 1,004미터, 설악산을 넘는 고갯길입니다.",
            descriptionOnly("오른쪽 창밖, 한계령. 해발 1,004미터, 설악산을 넘는 고갯길입니다.", "한계령"),
        )
        assertEquals(
            "동해를 굽어보는 천년 고찰입니다.",
            descriptionOnly("왼쪽 창밖, 낙산사. 동해를 굽어보는 천년 고찰입니다.", "낙산사"),
        )
    }

    @Test
    fun `앞머리가 없으면 그대로 둔다`() {
        val text = "해발 1,004미터, 설악산을 넘는 고갯길입니다."
        assertEquals(text, descriptionOnly(text, "한계령"))
    }

    @Test
    fun `이름이 다르면 떼지 않는다`() {
        val text = "오른쪽 창밖, 미시령. 울산바위가 솟아 있습니다."
        assertEquals(text, descriptionOnly(text, "한계령"))
    }

    @Test
    fun `이름에 정규식 기호가 있어도 글자 그대로 본다`() {
        assertEquals("설명.", descriptionOnly("왼쪽 창밖, 전망대(A+). 설명.", "전망대(A+)"))
    }
}

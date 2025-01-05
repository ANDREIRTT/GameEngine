package com.mal.game_engine.engine.opengl.shape.line

import android.content.Context
import android.opengl.GLES30
import com.mal.game_engine.engine.R
import com.mal.game_engine.engine.coordinate.Coordinate
import com.mal.game_engine.engine.opengl.BaseShape
import com.mal.game_engine.engine.opengl.readRawResource
import com.mal.game_engine.engine.opengl.toGLBuffer
import com.mal.game_engine.engine.opengl.withGL30
import java.nio.FloatBuffer
import kotlin.math.sqrt

class LineShape(
    private val context: Context
) : BaseShape<LineShapeData>(
    vertexShaderCode = context.readRawResource(R.raw.line_vt),
    fragmentShaderCode = context.readRawResource(R.raw.line_fg)
) {
    override fun draw(data: LineShapeData) {
        GLES30.glUseProgram(program)

        val coordinatesBuffer: FloatBuffer = createThickLine(
            data.startCoordinate,
            data.endCoordinate,
            data.thickness
        ).toGLBuffer()

        val positionHandle = GLES30.glGetAttribLocation(program, "aPosition").withGL30 {
            GLES30.glVertexAttribPointer(
                it,
                2,
                GLES30.GL_FLOAT,
                false,
                2 * 4,
                coordinatesBuffer
            )
            GLES30.glEnableVertexAttribArray(it)
        }

        GLES30.glGetUniformLocation(program, "uMVPMatrix").withGL30 {
            GLES30.glUniformMatrix4fv(it, 1, false, data.vpMatrix, 0)
        }

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(positionHandle)
    }

    private fun createThickLine(
        start: Coordinate,
        end: Coordinate,
        thickness: Double
    ): DoubleArray {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = sqrt(dx * dx + dy * dy)

        // Нормализованный перпендикулярный вектор
        val nx = -dy / length
        val ny = dx / length

        val halfThickness = thickness / 2.0

        // Четыре вершины прямоугольника
        return doubleArrayOf(
            start.x + nx * halfThickness, start.y + ny * halfThickness,  // Верхний левый
            start.x - nx * halfThickness, start.y - ny * halfThickness,  // Нижний левый
            end.x + nx * halfThickness, end.y + ny * halfThickness,      // Верхний правый
            end.x - nx * halfThickness, end.y - ny * halfThickness       // Нижний правый
        )
    }
}
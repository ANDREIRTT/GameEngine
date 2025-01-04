package com.mal.component.opengl.core.shape.rectangle

import android.content.Context
import android.opengl.GLES30
import com.mal.game_engine.engine.R
import com.mal.game_engine.engine.opengl.BaseShape
import com.mal.game_engine.engine.opengl.readRawResource
import com.mal.game_engine.engine.opengl.toGLBuffer
import com.mal.game_engine.engine.opengl.withGL30
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class RectangleShape(
    context: Context,
) : BaseShape<RectangleShapeData>(
    vertexShaderCode = context.readRawResource(R.raw.rectangle_vt),
    fragmentShaderCode = context.readRawResource(R.raw.rectangle_fg)
) {

    private val drawOrderBuffer: ShortBuffer = shortArrayOf(0, 1, 2, 0, 2, 3).toGLBuffer()


    override fun draw(data: RectangleShapeData) {
        GLES30.glUseProgram(program)
        val coordinatesBuffer: FloatBuffer = with(data) {
            val halfWidth = width / 2.0
            val halfHeight = height / 2.0
            doubleArrayOf(
                //x,    y
                coordinate.x - halfWidth, coordinate.y + halfHeight,
                coordinate.x - halfWidth, coordinate.y - halfHeight,
                coordinate.x + halfWidth, coordinate.y - halfHeight,
                coordinate.x + halfWidth, coordinate.y + halfHeight,
            ).toGLBuffer()
        }

        val quadPositionHandle = GLES30.glGetAttribLocation(program, "aPosition").withGL30 {
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


        val texPositionHandle = GLES30.glGetAttribLocation(program, "aTexCoord").withGL30 {
            GLES30.glVertexAttribPointer(
                it,
                2,
                GLES30.GL_FLOAT,
                false,
                2 * 4,
                data.texture.getTextureBuffer()
            )
            GLES30.glEnableVertexAttribArray(it)
        }

        GLES30.glGetUniformLocation(program, "uTexture").withGL30 {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, data.texture.glGenID)
            GLES30.glUniform1i(it, 0)
        }


        GLES30.glGetUniformLocation(program, "uMVPMatrix").withGL30 {
            GLES30.glUniformMatrix4fv(it, 1, false, data.vpMatrix, 0)
        }

        GLES30.glDrawElements(
            GLES30.GL_TRIANGLES,
            6,
            GLES30.GL_UNSIGNED_SHORT,
            drawOrderBuffer
        )

        GLES30.glDisableVertexAttribArray(quadPositionHandle)
        GLES30.glDisableVertexAttribArray(texPositionHandle)
    }
}
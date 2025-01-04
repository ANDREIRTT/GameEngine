package com.mal.game_engine.engine.opengl.shape.circle

import android.content.Context
import android.opengl.GLES30
import com.mal.component.opengl.core.shape.circle.CircleShapeData
import com.mal.game_engine.engine.R
import com.mal.game_engine.engine.opengl.BaseShape
import com.mal.game_engine.engine.opengl.readRawResource
import com.mal.game_engine.engine.opengl.toFloatArray
import com.mal.game_engine.engine.opengl.toGLBuffer
import com.mal.game_engine.engine.opengl.withGL30
import java.nio.FloatBuffer

class CircleShape(
    val context: Context,
    val segments: Int = 100
) : BaseShape<CircleShapeData>(
    vertexShaderCode = context.readRawResource(R.raw.circle_vt),
    fragmentShaderCode = context.readRawResource(R.raw.circle_fg),
) {

    private val vertexBuffer: FloatBuffer = run {
        val indices = FloatArray(segments)
        for (i in 0 until segments) {
            indices[i] = i.toFloat()
        }
        indices.toGLBuffer()
    }
    private val vertexStride: Int = 4 // 4 bytes per vertex (float)


    override fun draw(data: CircleShapeData) {
        GLES30.glUseProgram(program)

        val positionHandle = GLES30.glGetAttribLocation(program, "vSegment").withGL30 {
            GLES30.glEnableVertexAttribArray(it)
            GLES30.glVertexAttribPointer(
                it, 1,
                GLES30.GL_FLOAT, false,
                vertexStride, vertexBuffer
            )
        }

        GLES30.glGetUniformLocation(program, "uMVPMatrix").withGL30 {
            GLES30.glUniformMatrix4fv(it, 1, false, data.vpMatrix, 0)
        }

        GLES30.glGetUniformLocation(program, "uNumSegments").withGL30 {
            GLES30.glUniform1i(it, segments)
        }

        GLES30.glGetUniformLocation(program, "uRadius").withGL30 {
            GLES30.glUniform1f(it, data.radius.toFloat())
        }

        GLES30.glGetUniformLocation(program, "uCirclePosition").withGL30 {
            GLES30.glUniform2fv(
                it, 1, doubleArrayOf(
                    data.coordinate.x, data.coordinate.y
                ).toFloatArray(), 0
            )
        }

        GLES30.glGetUniformLocation(program, "uCircleBorderWidth").withGL30 {
            GLES30.glUniform1f(it, data.borderWidth.toFloat())
        }

        GLES30.glGetUniformLocation(program, "uCircleTexture").withGL30 {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, data.textureData.texture.glGenID)
            GLES30.glUniform1i(it, 0)
        }
        GLES30.glGetUniformLocation(
            program,
            "uTextureTransform"
        ).withGL30 {
            GLES30.glUniformMatrix3fv(
                it,
                1,
                false,
                data.textureData.texture.getTransformationMatrix(
                    data.textureData.textureTransformation
                ),
                0
            )
        }

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, segments)

        GLES30.glDisableVertexAttribArray(positionHandle)
    }
}
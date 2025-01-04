package com.mal.game_engine.engine.opengl

import android.opengl.GLES30

abstract class BaseShape<Data>(
    private val vertexShaderCode: String,
    private val fragmentShaderCode: String
) {
    var program: Int = -1
        private set

    private var vertexShader: Int = -1
    private var fragmentShader: Int = -1

    abstract fun draw(data: Data)

    fun create() {
        program = GLES30.glCreateProgram().withGL30 { program ->
            // add the vertex shader to program
            GLES30.glAttachShader(
                program,
                loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode)
            )

            // add the fragment shader to program
            GLES30.glAttachShader(
                program,
                loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode)
            )

            // creates OpenGL ES program executables
            GLES30.glLinkProgram(program)

            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                val log = GLES30.glGetProgramInfoLog(program)
                GLES30.glDeleteProgram(program)
                throw RuntimeException("Error linking program: $log")
            }
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES30.glCreateShader(type).withGL30 { shader ->
            GLES30.glShaderSource(shader, shaderCode)
            GLES30.glCompileShader(shader)

            val compileStatus = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                throw RuntimeException("Error compiling shader: " + GLES30.glGetShaderInfoLog(shader))
            }
        }
    }
}
package de.mineking.hexo.board.render.test

import de.mineking.hexo.board.render.image.BufferedImageBoardRenderer
import de.mineking.hexo.board.render.image.outputPngBytes

class PngRenderTest : AbstractImageRendererTest("png", BufferedImageBoardRenderer.Default.outputPngBytes())

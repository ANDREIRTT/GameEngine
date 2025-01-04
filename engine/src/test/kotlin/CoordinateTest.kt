import com.mal.game_engine.engine.coordinate.Coordinate
import org.junit.Assert
import org.junit.Test

class CoordinateTest {
    @Test
    fun testToVpCoordinate() {
        val startCoordinate = Coordinate(1.0, 1.0)
        val result = startCoordinate.applyProjection(0.68)
        Assert.assertEquals(
            Coordinate(
                0.68, 1.0
            ),
            result
        )
        Assert.assertEquals(
            startCoordinate,
            result.removeProjection(0.68)
        )
    }

    @Test
    fun testToVpCoordinateRightSide() {
        val startCoordinate = Coordinate(-1.0, 1.0)
        val result = startCoordinate
            .applyProjection(0.68)
        Assert.assertEquals(
            Coordinate(
                -0.68, 1.0
            ),
            result
        )
        Assert.assertEquals(
            startCoordinate,
            result.removeProjection(0.68)
        )
    }

    @Test
    fun textVpZeroCoordinate() {
        val startCoordinate = Coordinate(-0.0, 1.0)
        val result = startCoordinate
            .applyProjection(0.68)
        Assert.assertEquals(
            Coordinate(
                -0.0, 1.0
            ),
            result
        )
    }
}
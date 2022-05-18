package ga.rubydesic.dmd.util

import net.minecraft.util.math.BlockPos

operator fun BlockPos.component1() = x
operator fun BlockPos.component2() = y
operator fun BlockPos.component3() = z

fun Int.squared() = this * this
fun Int.pow(power: Int): Int {
    require(power >= 0)

    var result = 1
    repeat(power) { result *= this }
    return result
}

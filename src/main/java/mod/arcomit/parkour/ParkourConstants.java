package mod.arcomit.parkour;

/**
 * 跑酷模组跨类共享常量。
 *
 * <p>本类集中管理在多个类中重复出现且代表同一业务概念的魔法值。
 * 仅本类内部使用的常量应定义为 {@code private static final} 放在各自类中。
 *
 * @author Mitok
 * @since 2026-06-09
 */
public final class ParkourConstants {

    private ParkourConstants() {
        throw new UnsupportedOperationException("Constant classes cannot be instantiated");
    }

    /** AABB碰撞盒微缩/膨胀边距，防止相邻方块检测盒边界重叠导致误判 */
    public static final double AABB_DEFLATE_EPSILON = 0.001;

    /** 蜷缩姿态碰撞箱尺寸（宽度/高度，单位：格），用于滑铲和翻滚等压低姿态的状态 */
    public static final float CROUCH_HITBOX_SIZE = 0.6F;

    /** 蜷缩姿态视点高度（单位：格），模拟低头蜷缩的第一人称视角 */
    public static final float CROUCH_EYE_HEIGHT = 0.4F;

    /** 墙面吸附力，每tick沿墙面法线方向将玩家推向墙体，防止浮点漂移导致飘离 */
    public static final double WALL_ADHESION_FORCE = 0.1;

    /** 悬挂抓握高度比例（基于玩家身高），用于确定碰撞检测盒和音效定位的垂直偏移量 */
    public static final double ARMHANG_GRIP_HEIGHT_RATIO = 0.34;

    /** 悬挂传感器水平检测偏移距离（单位：格） */
    public static final double ARMHANG_SENSOR_CHECK_DISTANCE = 0.5;

    /** 状态退出后速度提升效果的持续时长（单位：刻，40刻=2秒） */
    public static final int EXIT_SPEED_BOOST_DURATION = 40;

    /** 状态退出后速度提升效果的增幅等级（0=速度I） */
    public static final int EXIT_SPEED_BOOST_AMPLIFIER = 0;

    /** 零阈值 */
    public static final double ZERO_THRESHOLD = 1.0E-7;
}

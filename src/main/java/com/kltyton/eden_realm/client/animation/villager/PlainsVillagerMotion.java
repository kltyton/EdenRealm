package com.kltyton.eden_realm.client.animation.villager;

import java.util.Arrays;
import java.util.Random;

/**
 * Per-entity procedural animation state, independent of Minecraft render objects.
 * Input and pose rotations use Minecraft-facing degrees; the renderer converts X/Y signs.
 * Translations use GeckoLib model units (1/16 block).
 * Only the 15 empty helper bones are written, never authored animation bones.
 */
public final class PlainsVillagerMotion {
    public static final String[] BONES = {
            "alive_body", "alive_head", "alive_eye_l", "alive_eye_r",
            "alive_lid_l", "alive_lid_r", "alive_brow_l", "alive_brow_r",
            "alive_ear_l", "alive_ear_r", "alive_leaves", "alive_nose",
            "alive_skirt_l", "alive_skirt_r", "alive_skirt_front"
    };
    public static final double HEAD_RESPONSE = 8.0;
    public static final double BODY_RESPONSE = 3.5;
    public static final double EYE_RESPONSE = 28.0;
    public static final double EYE_X_LIMIT = 0.36;
    public static final double EYE_Y_LIMIT = 0.18;
    // GeckoLib mirrors translation X before the entity renderer's 180-degree body rotation.
    public static final double EYE_X_SIGN = -1.0;
    public static final double EYE_Y_SIGN = -1.0;

    public record Input(double timeSeconds, double targetYaw, double targetPitch,
                        boolean hasTarget, double speed, double lookWeight,
                        double blinkWeight, double secondaryWeight,
                        double authoredHeadYaw, double authoredHeadPitch,
                        double bodyWorldYaw, boolean disabled) {
        public Input {
            if (!Double.isFinite(timeSeconds) || !Double.isFinite(targetYaw)
                    || !Double.isFinite(targetPitch) || !Double.isFinite(speed)
                    || !Double.isFinite(lookWeight) || !Double.isFinite(blinkWeight)
                    || !Double.isFinite(secondaryWeight) || !Double.isFinite(authoredHeadYaw)
                    || !Double.isFinite(authoredHeadPitch) || !Double.isFinite(bodyWorldYaw))
                throw new IllegalArgumentException("Animation inputs must be finite");
        }
    }

    /**
     * Reused mutable buffer owned by this state. Do not retain it as a historical snapshot.
     */
    public static final class Pose {
        private final double[][] channels = new double[BONES.length][6];

        public double get(int bone, int channel) {
            return channels[bone][channel];
        }

        private void clear() {
            for (double[] v : channels) Arrays.fill(v, 0);
        }

        private void rot(int i, double x, double y, double z) {
            channels[i][0] = x;
            channels[i][1] = y;
            channels[i][2] = z;
        }

        private void pos(int i, double x, double y, double z) {
            channels[i][3] = x;
            channels[i][4] = y;
            channels[i][5] = z;
        }
    }

    private final Random random;
    private final double phase, breathHz;
    private final Pose pose = new Pose();
    private final Spring leafX = new Spring(), leafZ = new Spring();
    private final Spring ear = new Spring(), nose = new Spring();
    private double lastTime = Double.NaN;
    private double look = 0, blinkGate = 0, secondary = 0, speed = 0;
    private double headYaw, headPitch, bodyYaw, eyeX, eyeY;
    private double lastVisualYaw, lastVisualPitch;
    private double glanceYaw, glancePitch, nextGlance;
    private double microX, microY, nextMicro;
    private double nextBlink, blinkStart = -1000;
    private int doubleBlinksLeft;
    private boolean wasDisabled;

    public PlainsVillagerMotion(long seed) {
        random = new Random(seed);
        phase = random.nextDouble() * Math.PI * 2;
        breathHz = .24 + random.nextDouble() * .08;
    }

    public Pose update(Input in) {
        double now = in.timeSeconds();
        if (in.disabled()) {
            pose.clear();
            if (!wasDisabled) {
                resetMotion();
                nextBlink = now + range(1.2, 3.8);
                nextGlance = now + .5;
                nextMicro = now + .5;
                blinkStart = -1000;
            }
            lastTime = now;
            wasDisabled = true;
            return pose;
        }
        if (wasDisabled) {
            wasDisabled = false;
            lastTime = Double.NaN;
        }
        // A second render pass, pause or a repeated timestamp must not advance timers.
        if (Double.isFinite(lastTime) && now == lastTime) return pose;
        double dt = now - lastTime;
        boolean discontinuity = !Double.isFinite(lastTime) || dt < 0 || dt > .5;
        if (discontinuity) {
            resetMotion();
            nextBlink = now + range(.8, 3.8);
            nextGlance = now + range(.5, 2);
            nextMicro = now + range(.2, .9);
            blinkStart = -1000;
            look = clamp(in.lookWeight(), 0, 1);
            blinkGate = clamp(in.blinkWeight(), 0, 1);
            secondary = clamp(in.secondaryWeight(), 0, 1);
            lastVisualYaw = in.bodyWorldYaw() + in.authoredHeadYaw();
            lastVisualPitch = in.authoredHeadPitch();
            dt = 0;
        }
        lastTime = now;
        look = damp(look, clamp(in.lookWeight(), 0, 1), 12, dt);
        blinkGate = damp(blinkGate, clamp(in.blinkWeight(), 0, 1), 18, dt);
        secondary = damp(secondary, clamp(in.secondaryWeight(), 0, 1), 12, dt);
        speed = damp(speed, clamp(in.speed(), 0, 1), 9, dt);
        if (now >= nextGlance) {
            glanceYaw = range(-8, 8);
            glancePitch = range(-3.5, 2.5);
            nextGlance = now + range(2.1, 5.4);
        }
        if (now >= nextMicro) {
            microX = range(-.035, .035);
            microY = range(-.02, .02);
            nextMicro = now + range(.8, 2.1);
        }
        double targetY = in.hasTarget() ? clamp(wrap(in.targetYaw()), -75, 75) : glanceYaw * (1 - speed);
        double targetP = in.hasTarget() ? clamp(in.targetPitch(), -30, 38) : glancePitch * (1 - speed);
        // Eyes respond rapidly; neck follows; chest takes a small, slower share.
        bodyYaw = damp(bodyYaw, clamp(targetY * .12, -8, 8) * look * (1 - .65 * speed), BODY_RESPONSE, dt);
        headYaw = damp(headYaw, clamp(targetY * .90 - bodyYaw, -55, 55) * look, HEAD_RESPONSE, dt);
        headPitch = damp(headPitch, clamp(targetP * .87, -26, 33) * look, HEAD_RESPONSE, dt);
        // Retain authored nods/poses. Residual includes their approximate summed yaw/pitch.
        double residualY = targetY - headYaw - bodyYaw - in.authoredHeadYaw();
        double residualP = targetP - headPitch - in.authoredHeadPitch();
        eyeX = damp(eyeX, clamp(EYE_X_SIGN * residualY * .018 + microX, -EYE_X_LIMIT, EYE_X_LIMIT) * look, EYE_RESPONSE, dt);
        eyeY = damp(eyeY, clamp(EYE_Y_SIGN * residualP * .012 + microY, -EYE_Y_LIMIT, EYE_Y_LIMIT) * look, EYE_RESPONSE, dt);
        if (now >= nextBlink) {
            blinkStart = now;
            if (doubleBlinksLeft > 0) {
                doubleBlinksLeft--;
                nextBlink = now + range(2.4, 5.8);
            } else if (random.nextDouble() < .13) {
                doubleBlinksLeft = 1;
                nextBlink = now + .33;
            } else nextBlink = now + range(2.4, 5.8);
        }
        // Closing .055 s, closed .025 s, reopening .12 s. A small right-eye delay.
        double leftBlink = blinkCurve(now - blinkStart) * blinkGate;
        double rightBlink = blinkCurve(now - blinkStart - .012) * blinkGate;
        double breath = Math.sin(now * breathHz * 2 * Math.PI + phase);
        double slow = Math.sin(now * .63 + phase * .73);
        double visualYaw = in.bodyWorldYaw() + in.authoredHeadYaw() + headYaw + bodyYaw;
        double visualPitch = in.authoredHeadPitch() + headPitch;
        double yawSpeed = dt > 0 ? clamp(wrap(visualYaw - lastVisualYaw) / dt, -160, 160) : 0;
        double pitchSpeed = dt > 0 ? clamp((visualPitch - lastVisualPitch) / dt, -120, 120) : 0;
        lastVisualYaw = visualYaw;
        lastVisualPitch = visualPitch;
        // Exact damped-spring integration; bounded goals, no high-FPS Euler instability.
        leafX.step(clamp(-pitchSpeed * .035, -5, 5), 16, .65, dt);
        leafZ.step(clamp(-yawSpeed * .045, -7, 7), 14, .62, dt);
        ear.step(clamp(-yawSpeed * .027, -4.2, 4.2), 19, .65, dt);
        nose.step(clamp(-pitchSpeed * .018, -2.5, 2.5), 21, .7, dt);
        pose.clear();
        pose.rot(0, .35 * breath * secondary, bodyYaw, .2 * slow * secondary);
        pose.pos(0, 0, .035 * breath * secondary, 0);
        pose.rot(1, headPitch, headYaw, .65 * slow * secondary * (1 - speed));
        pose.pos(2, eyeX, eyeY, 0);
        pose.pos(3, eyeX, eyeY, 0);
        pose.pos(4, 0, -2 * leftBlink, 0);
        pose.pos(5, 0, -2 * rightBlink, 0);
        double interest = in.hasTarget() ? .08 : 0;
        pose.pos(6, 0, (interest + .045 * slow) * secondary - .1 * leftBlink, 0);
        pose.pos(7, 0, (interest - .035 * slow) * secondary - .1 * rightBlink, 0);
        pose.rot(6, 0, 0, .7 * slow * secondary);
        pose.rot(7, 0, 0, -.55 * slow * secondary);
        pose.rot(8, .35 * breath * secondary, ear.x * secondary, (ear.x + .65 * Math.sin(now * 1.6 + phase)) * secondary);
        pose.rot(9, -.28 * breath * secondary, ear.x * secondary, (ear.x - .55 * Math.sin(now * 1.6 + phase + .5)) * secondary);
        pose.rot(10, (leafX.x + .7 * Math.sin(now * 1.9 + phase)) * secondary, 0, (leafZ.x + .8 * Math.sin(now * 1.4 + phase)) * secondary);
        pose.rot(11, nose.x * secondary, 0, -.25 * ear.x * secondary);
        pose.rot(12, .55 * Math.sin(now * 1.1 + phase) * secondary, 0, -.25 * ear.x * secondary);
        pose.rot(13, .55 * Math.sin(now * 1.1 + phase + .5) * secondary, 0, -.25 * ear.x * secondary);
        pose.rot(14, .3 * Math.sin(now * 1.1 + phase + .8) * secondary, 0, 0);
        return pose;
    }

    private void resetMotion() {
        headYaw = headPitch = bodyYaw = eyeX = eyeY = 0;
        doubleBlinksLeft = 0;
        leafX.clear();
        leafZ.clear();
        ear.clear();
        nose.clear();
    }

    private double range(double lo, double hi) {
        return lo + random.nextDouble() * (hi - lo);
    }

    public static double clamp(double x, double lo, double hi) {
        return Math.max(lo, Math.min(hi, x));
    }

    public static double wrap(double x) {
        return x - Math.floor((x + 180) / 360) * 360;
    }

    public static double damp(double current, double target, double rate, double dt) {
        return current + (target - current) * (-Math.expm1(-rate * Math.max(0, dt)));
    }

    public static double blinkCurve(double t) {
        if (t < 0 || t >= .2) return 0;
        if (t < .055) return smooth(t / .055);
        if (t < .08) return 1;
        return 1 - smooth((t - .08) / .12);
    }

    private static double smooth(double x) {
        x = clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    private static final class Spring {
        double x, v;

        void clear() {
            x = v = 0;
        }

        void step(double target, double omega, double zeta, double dt) {
            if (dt <= 0) return;
            double a = zeta * omega, b = omega * Math.sqrt(1 - zeta * zeta);
            double y = x - target, c = (v + a * y) / b, decay = Math.exp(-a * dt);
            double co = Math.cos(b * dt), si = Math.sin(b * dt);
            x = target + decay * (y * co + c * si);
            v = decay * (v * co - (a * c + b * y) * si);
        }
    }
}

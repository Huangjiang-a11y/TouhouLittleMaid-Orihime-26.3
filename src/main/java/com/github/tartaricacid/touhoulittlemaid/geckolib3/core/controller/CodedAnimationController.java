package com.github.tartaricacid.touhoulittlemaid.geckolib3.core.controller;

import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.controller.IAnimationPredicate;
import com.github.tartaricacid.touhoulittlemaid.client.animation.gecko.molang.CtrlBinding;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.AnimatableEntity;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.AnimationState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.PlayState;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.builder.LoopType;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.controller.transition.IBlendTransition;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.controller.transition.LinearBlendTransition;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.event.AnimationEvent;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.keyframe.AnimationVec3;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.keyframe.BoneAnimationQueue;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.keyframe.point.BeginningTransitionPoint;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.keyframe.point.EndingTransitionPoint;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.context.ControllerContext;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.context.MolangContext;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.molang.value.IValue;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.snapshot.BoneTopLevelSnapshot;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.util.MathUtil;
import com.github.tartaricacid.touhoulittlemaid.molang.runtime.ExpressionEvaluator;
import it.unimi.dsi.fastutil.objects.Object2ReferenceMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class CodedAnimationController<T extends AnimatableEntity<?>> implements IAnimationController<T> {
    private final String name;
    private final IAnimationPredicate<T> animationPredicate;
    private final AnimationPlayer animationPlayer;
    private final boolean blendRotation;
    private final ControllerContext ctx;
    private final float defaultTransitionTicks;
    @Nullable
    private IValue molangPredicate;
    private boolean pause;

    /**
     * 实例化硬编码动画控制器，每个控制器同一时间只能播放一个动画 <br>
     * 你可以为一个实体附加多个动画控制器 <br>
     * 比如一个控制器控制实体大小，另一个控制移动，攻击等等
     *
     * @param animatableEntity      实体
     * @param name                  动画控制器名称
     * @param transitionLengthTicks 动画过渡时间（tick）
     */
    public CodedAnimationController(T animatableEntity, String name, float transitionLengthTicks,
                                    IAnimationPredicate<T> animationPredicate) {
        this(animatableEntity, name, transitionLengthTicks, animationPredicate, false);
    }

    @Deprecated
    public CodedAnimationController(T animatableEntity, String name, float transitionLengthTicks,
                                    IAnimationPredicate<T> animationPredicate, boolean blendRotation) {
        this.name = name;
        this.animationPredicate = animationPredicate;
        this.animationPlayer = new AnimationPlayer(animatableEntity, transitionLengthTicks, true);
        this.defaultTransitionTicks = transitionLengthTicks;
        this.blendRotation = blendRotation;
        this.ctx = new ControllerContext(false);
    }

    @Override
    public void process(AnimationEvent<T> event, ExpressionEvaluator<MolangContext<?>> evaluator, boolean allowEmitting) {
        event.setCodedAnimationController(this);
        PlayState playState = evalMolangPredicate(evaluator);
        if (playState == null) {
            playState = this.animationPredicate.test(event);
        }
        event.setCodedAnimationController(null);

        if (playState == PlayState.CONTINUE) {
            this.animationPlayer.process(event.getRenderTicks(), evaluator, allowEmitting);
            this.pause = false;
        } else if (playState == PlayState.STOP) {
            var state = this.animationPlayer.getState();
            if (state == AnimationState.BEGINNING_TRANSITION || state == AnimationState.RUNNING) {
                this.animationPlayer.stop(event.getRenderTicks());
                this.animationPlayer.indicateReload();
            }
            if (state == AnimationState.ENDING_TRANSITION) {
                this.animationPlayer.process(event.getRenderTicks(), evaluator, allowEmitting);
            }
            this.pause = false;
        } else if (playState == PlayState.PAUSE) {
            this.animationPlayer.process(event.getRenderTicks(), evaluator, false);
            this.animationPlayer.resetBoneAnimationQueues();
            this.pause = true;
        }

        // 最后给 MoLang 上下文设置当前动画状态
        event.getAnimatableEntity().setCodedAnimationStates(this.name, this.animationPlayer.getState());
    }

    @Nullable
    private PlayState evalMolangPredicate(ExpressionEvaluator<MolangContext<?>> evaluator) {
        if (this.molangPredicate == null) {
            return null;
        }

        this.ctx.setAnyAnimationFinished(this.animationPlayer.currentAnimFinished());
        this.ctx.setAllAnimationsFinished(this.animationPlayer.currentAnimFinished());

        evaluator.entity().setControllerContext(this.ctx);
        evaluator.entity().setAnimationContext(this.animationPlayer.getAnimationContext());
        evaluator.entity().setAllowEmitting(true);

        var state = this.molangPredicate.evalAsInt(evaluator);

        evaluator.entity().setAllowEmitting(false);
        evaluator.entity().setAnimationContext(null);
        evaluator.entity().setControllerContext(null);

        return switch (state) {
            case CtrlBinding.STATE_CONTINUE -> PlayState.CONTINUE;
            case CtrlBinding.STATE_PAUSE -> PlayState.PAUSE;
            case CtrlBinding.STATE_STOP -> PlayState.STOP;
            default -> null;
        };
    }

    @Override
    public void updateModel(List<BoneTopLevelSnapshot> modelBones, Object2ReferenceMap<String, List<IValue>> eventHandlers) {
        this.animationPlayer.updateModel(modelBones);
        this.animationPlayer.setBeginningTransition(new LinearBlendTransition(defaultTransitionTicks));
        this.molangPredicate = null;
        var predictEventName = this.name.replace(".", "_ctrl_");
        var handlers = eventHandlers.get(predictEventName);
        if (handlers != null && !handlers.isEmpty()) {
            this.molangPredicate = handlers.getFirst();
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    public String getStateName() {
        // 硬编码控制器没有状态，返回自己名称+正在播放的动画
        if (animationPlayer.getState() == AnimationState.IDLE) {
            return "Coded";
        } else {
            return "Coded -> " + animationPlayer.getCurrentAnim().animationName();
        }
    }

    public void setAnimation(String animationName) {
        this.animationPlayer.setAnimation(animationName, null);
    }

    public void setAnimation(String animationName, @Nullable LoopType loopType) {
        this.animationPlayer.setAnimation(animationName, loopType);
    }

    public void setBeginningTransitionLength(float sec) {
        if (this.animationPlayer.getBeginningTransitionLength() != sec) {
            this.animationPlayer.setBeginningTransition(new LinearBlendTransition(sec));
        }
    }

    public void setBeginningTransition(IBlendTransition blendTransition) {
        this.animationPlayer.setBeginningTransition(blendTransition);
    }

    public void finalizeAnimationContext(ExpressionEvaluator<MolangContext<?>> evaluator) {
        this.animationPlayer.finalizeAnimationContext(evaluator);
    }

    @Override
    public void visitBoneAnimationQueues(Consumer<IBoneAnimationQueue> visitor) {
        if (!this.pause) {
            for (var queue : this.animationPlayer.getActiveBoneAnimQueues()) {
                visitor.accept(new SingleBoneAnimationQueue(queue));
            }
        }
    }

    public void reset() {
        this.animationPlayer.reset();
    }

    public void clear() {
        this.animationPlayer.clear();
        this.molangPredicate = null;
    }

    public void indicateReload() {
        this.animationPlayer.indicateReload();
    }

    public void stop(float renderTicks) {
        this.animationPlayer.stop(renderTicks);
    }

    public boolean isAnimFinished() {
        return this.animationPlayer.currentAnimFinished();
    }

    public void stopPlayingSounds() {
        this.animationPlayer.stopPlayingSounds();
    }

    @Override
    @Deprecated
    public boolean blendRotation() {
        // 旧版 geckolib 语义：本次旋转以"累加"方式参与混合（高并行动画在用），而不是覆盖。
        //
        // 过渡期间必须关掉累加，否则收尾过渡会残留在累加值里：
        // AnimationProcessor 中 blendRotation 为真时走 pointData.add(rot)，而 pointData 保存的是
        // 上一次的累加结果；过渡淡出时 rot 本身会衰减，但 pointData 不会跟着衰减，
        // 于是骨骼会卡在最后一次的旋转上。关掉后退回 applyRotation（覆盖语义），
        // 过渡就能正确地淡回默认姿态；起始过渡同理，从骨骼初始旋转淡入。
        // 因此这里的状态判断不是"缓解"，而是累加混合在过渡期的正确处理。
        return blendRotation && animationPlayer.getState() == AnimationState.RUNNING;
    }

    private record SingleBoneAnimationQueue(BoneAnimationQueue queue) implements IBoneAnimationQueue {
        @Override
        public BoneTopLevelSnapshot getSnapshot() {
            return queue.topLevelSnapshot;
        }

        @Override
        public Optional<AnimationVec3> pollRotationPoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
            var point = this.queue.rotation;

            if (point == null) {
                return Optional.empty();
            }

            AnimationVec3 pointValue;
            if (point instanceof EndingTransitionPoint endingPoint) {
                pointValue = new AnimationVec3(point.getLerpPoint(evaluator));
                pointValue.setEndingTransitionPercentProgressIfLess(endingPoint.getPercentCompleted());
                var weight = queue.getBlendWeight();
                if (weight != 1) {
                    pointValue.mul(weight);
                }
            } else if (point instanceof BeginningTransitionPoint beginningPoint) {
                var dst = beginningPoint.getTransitionDst(evaluator)
                        .mul(queue.getBlendWeight());
                MathUtil.lerpRotationValues(beginningPoint.getTransitionPercentProgress(),
                        beginningPoint.getTransitionOffset(),
                        dst,
                        queue.topLevelSnapshot.bone.getInitialRotation(),
                        dst);
                pointValue = new AnimationVec3(dst);
                pointValue.setEndingTransitionPercentProgressIfLess(0);
            } else {
                pointValue = new AnimationVec3(point.getLerpPoint(evaluator));
                var weight = queue.getBlendWeight();
                if (weight != 1) {
                    pointValue.mul(weight);
                }
                pointValue.setEndingTransitionPercentProgressIfLess(0);
            }

            return Optional.of(pointValue);
        }

        @Override
        public Optional<AnimationVec3> pollPositionPoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
            var point = this.queue.position;

            if (point == null) {
                return Optional.empty();
            }

            var pointValue = new AnimationVec3(point.getLerpPoint(evaluator));
            var weight = queue.getBlendWeight();
            if (point instanceof EndingTransitionPoint endingPoint) {
                pointValue.setEndingTransitionPercentProgressIfLess(endingPoint.getPercentCompleted());
            } else {
                if (point instanceof BeginningTransitionPoint beginningPoint) {
                    weight = MathUtil.lerpValues(beginningPoint.getTransitionPercentProgress(), 1, weight);
                }
                pointValue.setEndingTransitionPercentProgressIfLess(0);
            }

            if (weight != 1) {
                pointValue.mul(weight);
            }
            return Optional.of(pointValue);
        }

        @Override
        public Optional<AnimationVec3> pollScalePoint(ExpressionEvaluator<MolangContext<?>> evaluator) {
            var point = this.queue.scale;

            if (point == null) {
                return Optional.empty();
            }

            var pointValue = new AnimationVec3(point.getLerpPoint(evaluator));
            var weight = queue.getBlendWeight();
            if (point instanceof EndingTransitionPoint endingPoint) {
                pointValue.setEndingTransitionPercentProgressIfLess(endingPoint.getPercentCompleted());
            } else {
                if (point instanceof BeginningTransitionPoint beginningPoint) {
                    weight = MathUtil.lerpValues(beginningPoint.getTransitionPercentProgress(), 1, weight);
                }
                pointValue.setEndingTransitionPercentProgressIfLess(0);
            }

            if (weight != 1) {
                MathUtil.computeWeightedScale(pointValue, weight, pointValue);
            }
            return Optional.of(pointValue);
        }
    }
}

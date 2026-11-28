type Nullable<T> = T | null | undefined
declare function KtSingleton<T>(): T & (abstract new() => any);
export declare abstract class GritDomain {
    static readonly getInstance: () => typeof GritDomain.$metadata$.type;
    private constructor();
}
export declare namespace GritDomain {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        abstract class type extends KtSingleton<constructor>() {
            private constructor();
        }
        abstract class constructor {
            estimateE1rm(weightKg: number, reps: number, rpe: Nullable<number>): number;
            platesPerSide(totalKg: number, changePlates: boolean): Nullable<Array<number>>;
            roundLoad(kg: number, changePlates: boolean): number;
            loadStep(changePlates: boolean): number;
            checkInput(weightKg: number, reps: number, lastWeightKg: Nullable<number>): Array<string>;
            checkLoadInput(weight: number, reps: number, lastWeight: Nullable<number>, unit: string, plateCount: string): Array<string>;
            plateCountOffered(exercise: JsExercise): boolean;
            loadUnitOf(exercise: JsExercise): string;
            plateStep(plateCount: string): number;
            plateUp(plate: Nullable<number>, plateCount: string): number;
            plateDown(plate: Nullable<number>, plateCount: string): number;
            plateCheck(plate: number, plateCount: string): string;
            plateText(plate: number): string;
            plateSpoken(plate: number): string;
            maxPlate(): number;
            parseBaselineKg(text: string): Nullable<number>;
            targetsFor(weekday: number, history: JsHistory, plan: Array<JsPlanItem>, settings: JsCoachSettings, today: string, prior: Array<JsAdvice>): Array<JsAdvice>;
            evaluate(history: JsHistory, plan: Array<JsPlanItem>, settings: JsCoachSettings, today: string, prior: Array<JsAdvice>): Array<JsAdvice>;
            coachingTargetsFor(mode: string, weekday: number, history: JsHistory, plan: Array<JsPlanItem>, settings: JsCoachSettings, today: string, prior: Array<JsAdvice>): Array<JsAdvice>;
            coachingTargetsForWithSkips(mode: string, weekday: number, history: JsHistory, plan: Array<JsPlanItem>, settings: JsCoachSettings, today: string, prior: Array<JsAdvice>, skips: Array<JsSkip>): Array<JsAdvice>;
            coachingAfterSkip(skip: JsSkip, skips: Array<JsSkip>, history: JsHistory, plan: Array<JsPlanItem>, today: string, prior: Array<JsAdvice>): Array<JsAdvice>;
            coachingApplySkipSwap(payloadJson: string, choice: string, mode: string, plan: Array<JsPlanItem>, exercises: Array<JsExercise>): JsPlanChange;
            coachingEvaluate(mode: string, history: JsHistory, plan: Array<JsPlanItem>, settings: JsCoachSettings, today: string, prior: Array<JsAdvice>): Array<JsAdvice>;
            coachingEvaluateWithCheckIns(mode: string, history: JsHistory, plan: Array<JsPlanItem>, settings: JsCoachSettings, today: string, prior: Array<JsAdvice>, checkIns: Array<JsCheckIn>): Array<JsAdvice>;
            coachingCheckInDue(history: JsHistory, plan: Array<JsPlanItem>, checkIns: Array<JsCheckIn>, today: string): boolean;
            physiqueAfterCheckIn(checkIn: JsCheckIn, history: JsHistory, plan: Array<JsPlanItem>, today: string, prior: Array<JsAdvice>): JsCheckInOutcome;
            physiqueStuck(history: JsHistory, plan: Array<JsPlanItem>, checkIns: Array<JsCheckIn>, prior: Array<JsAdvice>, today: string): Array<JsAdvice>;
            physiqueApplyMoreWork(payloadJson: string, plan: Array<JsPlanItem>, exercises: Array<JsExercise>): JsPlanChange;
            physiqueApplySwap(payloadJson: string, choice: string, plan: Array<JsPlanItem>, exercises: Array<JsExercise>): JsPlanChange;
            physiqueWeeklySets(plan: Array<JsPlanItem>, exercises: Array<JsExercise>, group: string): number;
            muscleProgressRows(history: JsHistory, plan: Array<JsPlanItem>, checkIns: Array<JsCheckIn>, today: string): Array<JsMuscleRow>;
            welcomeBack(leftOn: Nullable<string>, today: string, hasPlan: boolean, firstSessionOn: Nullable<string>): boolean;
            coachingReEntryShows(mode: string, createdAt: string, history: JsHistory): boolean;
            repsToBeat(item: JsPlanItem, history: JsHistory, setNumber: number, today: string): number;
            repsToBeatWithSkips(item: JsPlanItem, history: JsHistory, setNumber: number, today: string, skips: Array<JsSkip>): number;
            bodybuildingProgress(history: JsHistory, plan: Array<JsPlanItem>, today: string): Array<JsExerciseTrend>;
            planBuilderBuild(days: Array<number>, picks: Array<JsBuilderPick>, priorities: Array<string>, variant: number): JsPlanDraft;
            planBuilderAlternatives(name: string, draft: JsPlanDraft, picks: Array<JsBuilderPick>, day: Nullable<number>): Array<JsLibraryExercise>;
            planBuilderSwap(draft: JsPlanDraft, day: number, from: string, to: string): JsPlanDraft;
            planBuilderSuggestedPicks(history: JsHistory, plans: Array<JsPlanItem>, today: string): Array<number>;
            planBuilderMaxPriorities(): number;
            planBuilderTargetGroups(): Array<string>;
            planTemplatesFor(mode: string): Array<JsPlanTemplate>;
            planTemplateSuggestedDays(count: number, current: Array<number>): Array<number>;
            planTemplateDraft(id: string, days: Array<number>, strictPress: boolean): JsPlanDraft;
            planTemplateAlternatives(id: string, draft: JsPlanDraft, day: number, name: string): Array<JsLibraryExercise>;
            planTemplateSwap(draft: JsPlanDraft, day: number, from: string, to: string): JsPlanDraft;
            weeklyTrend(history: JsHistory, lift: string, until: string): Array<JsWeeklyPoint>;
            currentE1rm(history: JsHistory, lift: string, until: string, days: number): Nullable<number>;
            prs(history: JsHistory, lift: string): Array<JsPr>;
            knownBest(history: JsHistory, lift: string): Nullable<number>;
            isNewPr(history: JsHistory, set: JsSet, date: string, mode: string): boolean;
            weeklyVolume(history: JsHistory, lift: string): Array<JsWeeklyPoint>;
            accessoryOptions(lift: string, miss: Nullable<string>): Array<JsAccessoryOption>;
            accessoryMuscles(lift: string, miss: Nullable<string>): Array<string>;
            seedExercises(): Array<JsExercise>;
            total(history: JsHistory, until: string, includeStrictPress: boolean): Nullable<number>;
            mainLifts(): Array<string>;
            liftLabel(lift: string): string;
            missPointLabel(lift: string, point: string): string;
            repsText(reps: number, repsHigh: Nullable<number>): string;
            mainLiftOf(exercise: JsExercise): Nullable<string>;
            isMainLift(exercise: JsExercise): boolean;
            lastTopSet(history: JsHistory, exerciseId: number, before: string): Nullable<JsSet>;
            lastTimes(history: JsHistory, mode: string, before: string): Array<JsLastTime>;
            supersetPairs(plan: Array<JsPlanItem>): Array<JsSupersetPair>;
            supersetCanLink(plan: Array<JsPlanItem>, itemId: number): boolean;
            supersetStale(plan: Array<JsPlanItem>): Array<number>;
            supersetBroken(before: Array<JsPlanItem>, after: Array<JsPlanItem>): Array<number>;
            supersetDue(firstDone: number, firstTarget: number, secondDone: number, secondTarget: number): Nullable<string>;
            supersetAfterSet(logged: string, firstDone: number, firstTarget: number, secondDone: number, secondTarget: number, keepCard: boolean): JsSupersetStep;
            warmUp(exercise: JsExercise, working: number, reps: number, changePlates: boolean): Array<JsWarmUpSet>;
            weeksInARow(workoutDays: Array<string>, today: string): number;
            openAdviceExpired(type: string, createdAt: string, today: string): boolean;
            advicePayload(payloadJson: string): Nullable<JsAdvicePayload>;
            library(): Array<JsLibraryExercise>;
            findInLibrary(name: string): Nullable<JsLibraryExercise>;
            isStrictPressUpgradeName(name: string): boolean;
            searchLibrary(query: string, areas: Array<string>, equipment: Array<string>): Array<JsLibraryExercise>;
            muscleGroups(): Array<string>;
            muscleGroupLabel(group: string): string;
            muscleGroupOf(muscle: string): Nullable<string>;
            exercisesByMuscle(group: string, equipment: Array<string>): Array<JsLibraryExercise>;
            exercisesByMuscles(groups: Array<string>, equipment: Array<string>): Array<JsLibraryExercise>;
            formGuide(name: string): Nullable<JsFormGuide>;
            figureInfo(name: string): Nullable<JsFigureInfo>;
            hasFigure(name: string): boolean;
            figureShapes(name: string, which: string): Nullable<Array<JsFigureShape>>;
            muscleFocus(name: string): Nullable<JsMuscleFocus>;
            safetyLine(): string;
            rpeGuide(): JsRpeGuide;
            ratioRanges(): Array<JsRatioRange>;
            modeSwitchDropTimeline(): JsDropTimeline;
            modeSwitchPumpTimeline(): JsPumpTimeline;
            modeSwitchVibration(): JsModeSwitchVibration;
            modeSwitchColors(): JsModeSwitchColors;
            modeSwitchLightColors(): JsModeSwitchLightColors;
            modeSwitchVeinEdgeWidth(coreWidth: number, widthScale: number): number;
            modeSwitchSparkWidth(): number;
            modeSwitchHammerPaths(): Array<JsMotionPath>;
            modeSwitchHammerRune(): JsMotionPath;
            modeSwitchHammerRunePaths(): Array<JsMotionPath>;
            modeSwitchGradients(): Array<JsMotionGradient>;
            modeSwitchHitPoint(w: number, h: number): JsMotionPoint;
            modeSwitchDropFrame(tMs: number, w: number, h: number): JsDropFrame;
            modeSwitchHop(tMs: number, elementCenterY: number, hitY: number): number;
            modeSwitchPumpFrame(tMs: number, w: number, h: number, srcX: number, srcY: number): JsPumpFrame;
            modeSwitchPumpElementScale(tMs: number, w: number, elementCenterY: number, srcX: number, srcY: number): number;
            modeSwitchCracks(w: number, h: number, hitX: number, hitY: number): Array<JsMotionPath>;
            modeSwitchDust(hitX: number, hitY: number): Array<JsMotionParticle>;
            modeSwitchSparks(hitX: number, hitY: number): Array<JsMotionParticle>;
            modeSwitchBurst(hitX: number, hitY: number): Array<JsMotionPoint>;
            modeSwitchVeins(w: number, h: number, srcX: number, srcY: number): Array<JsVeinPath>;
            modeSwitchVeinNodes(w: number, h: number, srcX: number, srcY: number): Array<JsVeinNode>;
            modeSwitchNodeShown(front: number, d0: number): boolean;
            modeSwitchVeinProgress(front: number, d0: number, len: number): number;
            modeSwitchPulseOpacity(pulseOpacity: number, front: number, len: number): number;
            modeSwitchPulseStart(front: number, len: number): number;
            guideScreens(): Array<string>;
            guideScreen(id: string, mode: string): JsGuideScreen;
            guideSteps(id: string, mode: string): Array<string>;
            guideStepMs(): number;
            guideMoveMs(): number;
            guideFadeMs(): number;
            guideStepStartMs(index: number): number;
            guideSettledMs(index: number): number;
            guideDoneMs(count: number): number;
            guideEase(x: number): number;
            guideTapMs(): number;
            guideTap(step: string): Nullable<string>;
            guideMotionStartMs(steps: Array<string>, index: number): number;
            guideFrame(id: string, mode: string, tMs: number): JsGuideFrame;
            guideSampleToday(): string;
            guideSampleTemplate(mode: string): JsPlanTemplate;
            guideSamplePlan(mode: string): Array<JsPlanItem>;
            guideSampleTodayItems(mode: string): Array<JsPlanItem>;
            guideSampleExercises(mode: string): Array<JsExercise>;
            guideSampleTodayEditor(mode: string, step: string, progress: number): JsGuideEditor;
            guideSampleTodaySet(mode: string): JsGuideTargetStep;
            guideSampleTargets(mode: string): Array<JsGuideTargetStep>;
            guideSampleAdvice(mode: string): Array<JsAdvice>;
            guideSampleProgress(mode: string): JsGuideProgress;
            guideSideFrame(tMs: number): JsGuideSideFrame;
            guideSideStill(): JsGuideSideFrame;
            guideSideLoopMs(): number;
            guideSideBox(): JsGuideSideBox;
            guideSampleSwap(mode: string): JsGuideSwap;
            guideSampleDraft(mode: string, swapped: boolean): JsPlanDraft;
            guideSampleFilledPlan(mode: string): Array<JsPlanItem>;
            guideSampleFilledExercises(mode: string): Array<JsExercise>;
            guideSampleTargetAdvice(mode: string): Array<JsAdvice>;
            guideSampleTargetHistory(mode: string): JsHistory;
            guideSampleTargetEditor(mode: string, index: number, progress: number): JsGuideEditor;
            guideSampleStallTrend(mode: string): Array<JsGuidePoint>;
            guideSampleStallE1rm(mode: string): Nullable<number>;
            guideSampleMonthsDate(mode: string, step: string, progress: number): string;
            guideSampleMonthsHistory(mode: string, until: string): JsHistory;
            guideSampleMonthsPlan(mode: string): Array<JsPlanItem>;
            guideSampleMonthsCheckIn(mode: string, step: string, progress: number): Nullable<JsCheckIn>;
            private constructor();
        }
    }
}
export declare class JsGuideLine {
    constructor(text: string, constants: Array<string>);
    get text(): string;
    get constants(): Array<string>;
    copy(text?: string, constants?: Array<string>): JsGuideLine;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsGuideLine {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsGuideLine;
    }
}
export declare class JsGuideScreen {
    constructor(id: string, title: string, lines: Array<JsGuideLine>, example: boolean);
    get id(): string;
    get title(): string;
    get lines(): Array<JsGuideLine>;
    get example(): boolean;
    copy(id?: string, title?: string, lines?: Array<JsGuideLine>, example?: boolean): JsGuideScreen;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsGuideScreen {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsGuideScreen;
    }
}
export declare class JsGuideTargetStep {
    constructor(date: string, exerciseId: string, doneWeightKg: number, doneReps: number, doneRpe: number, nextWeightKg: number, nextRepsLow: number, nextRepsHigh: number, note: Nullable<string>);
    get date(): string;
    get exerciseId(): string;
    get doneWeightKg(): number;
    get doneReps(): number;
    get doneRpe(): number;
    get nextWeightKg(): number;
    get nextRepsLow(): number;
    get nextRepsHigh(): number;
    get note(): Nullable<string>;
    copy(date?: string, exerciseId?: string, doneWeightKg?: number, doneReps?: number, doneRpe?: number, nextWeightKg?: number, nextRepsLow?: number, nextRepsHigh?: number, note?: Nullable<string>): JsGuideTargetStep;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsGuideTargetStep {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsGuideTargetStep;
    }
}
export declare class JsGuidePoint {
    constructor(date: string, value: number);
    get date(): string;
    get value(): number;
    copy(date?: string, value?: number): JsGuidePoint;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsGuidePoint {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsGuidePoint;
    }
}
export declare class JsGuideBest {
    constructor(exerciseId: string, kg: number);
    get exerciseId(): string;
    get kg(): number;
    copy(exerciseId?: string, kg?: number): JsGuideBest;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsGuideBest {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsGuideBest;
    }
}
export declare class JsGuideProgress {
    constructor(points: Array<JsGuidePoint>, bests: Array<JsGuideBest>, totalKg: Nullable<number>, laggingMuscle: Nullable<string>);
    get points(): Array<JsGuidePoint>;
    get bests(): Array<JsGuideBest>;
    get totalKg(): Nullable<number>;
    get laggingMuscle(): Nullable<string>;
    copy(points?: Array<JsGuidePoint>, bests?: Array<JsGuideBest>, totalKg?: Nullable<number>, laggingMuscle?: Nullable<string>): JsGuideProgress;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsGuideProgress {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsGuideProgress;
    }
}
export declare class JsGuideFrame {
    constructor(stepIndex: number, step: string, progress: number, done: boolean, tap: Nullable<string>, tapProgress: number);
    get stepIndex(): number;
    get step(): string;
    get progress(): number;
    get done(): boolean;
    get tap(): Nullable<string>;
    get tapProgress(): number;
    copy(stepIndex?: number, step?: string, progress?: number, done?: boolean, tap?: Nullable<string>, tapProgress?: number): JsGuideFrame;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsGuideFrame {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsGuideFrame;
    }
}
export declare class JsGuideEditor {
    constructor(weightKg: Nullable<number>, reps: number, rpe: number);
    get weightKg(): Nullable<number>;
    get reps(): number;
    get rpe(): number;
    copy(weightKg?: Nullable<number>, reps?: number, rpe?: number): JsGuideEditor;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsGuideEditor {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsGuideEditor;
    }
}
export declare class JsGuideSideFrame {
    constructor(dropMs: number, pumpMs: number);
    get dropMs(): number;
    get pumpMs(): number;
    copy(dropMs?: number, pumpMs?: number): JsGuideSideFrame;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsGuideSideFrame {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsGuideSideFrame;
    }
}
export declare class JsGuideSideBox {
    constructor(w: number, h: number, srcX: number, srcY: number);
    get w(): number;
    get h(): number;
    get srcX(): number;
    get srcY(): number;
    copy(w?: number, h?: number, srcX?: number, srcY?: number): JsGuideSideBox;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsGuideSideBox {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsGuideSideBox;
    }
}
export declare class JsGuideSwap {
    constructor(day: number, from: string, to: string);
    get day(): number;
    get from(): string;
    get to(): string;
    copy(day?: number, from?: string, to?: string): JsGuideSwap;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsGuideSwap {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsGuideSwap;
    }
}
export declare class JsMotionPoint {
    constructor(x: number, y: number);
    get x(): number;
    get y(): number;
    copy(x?: number, y?: number): JsMotionPoint;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsMotionPoint {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsMotionPoint;
    }
}
export declare class JsDropTimeline {
    constructor(fall: number, impact: number, hold: number, settle: number, lift: number, end: number, hammerScale: number);
    get fall(): number;
    get impact(): number;
    get hold(): number;
    get settle(): number;
    get lift(): number;
    get end(): number;
    get hammerScale(): number;
    copy(fall?: number, impact?: number, hold?: number, settle?: number, lift?: number, end?: number, hammerScale?: number): JsDropTimeline;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsDropTimeline {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsDropTimeline;
    }
}
export declare class JsPumpTimeline {
    constructor(beats: Array<number>, amps: Array<number>, rise: Array<number>, fall: Array<number>, reach: Array<number>, surge: Array<number>, flood: number, end: number);
    get beats(): Array<number>;
    get amps(): Array<number>;
    get rise(): Array<number>;
    get fall(): Array<number>;
    get reach(): Array<number>;
    get surge(): Array<number>;
    get flood(): number;
    get end(): number;
    copy(beats?: Array<number>, amps?: Array<number>, rise?: Array<number>, fall?: Array<number>, reach?: Array<number>, surge?: Array<number>, flood?: number, end?: number): JsPumpTimeline;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsPumpTimeline {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsPumpTimeline;
    }
}
export declare class JsModeSwitchVibration {
    constructor(dropPattern: Array<number>, dropAmplitudes: Array<number>, pumpPulses: Array<number>, pumpAmplitudes: Array<number>);
    get dropPattern(): Array<number>;
    get dropAmplitudes(): Array<number>;
    get pumpPulses(): Array<number>;
    get pumpAmplitudes(): Array<number>;
    copy(dropPattern?: Array<number>, dropAmplitudes?: Array<number>, pumpPulses?: Array<number>, pumpAmplitudes?: Array<number>): JsModeSwitchVibration;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsModeSwitchVibration {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsModeSwitchVibration;
    }
}
export declare class JsMotionPath {
    constructor(d: string, fill: Nullable<string>, stroke: Nullable<string>, strokeWidth: number, opacity: number, lineCap: string, lineJoin: string);
    get d(): string;
    get fill(): Nullable<string>;
    get stroke(): Nullable<string>;
    get strokeWidth(): number;
    get opacity(): number;
    get lineCap(): string;
    get lineJoin(): string;
    copy(d?: string, fill?: Nullable<string>, stroke?: Nullable<string>, strokeWidth?: number, opacity?: number, lineCap?: string, lineJoin?: string): JsMotionPath;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsMotionPath {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsMotionPath;
    }
}
export declare class JsGradientStop {
    constructor(offset: number, color: string, opacity: number);
    get offset(): number;
    get color(): string;
    get opacity(): number;
    copy(offset?: number, color?: string, opacity?: number): JsGradientStop;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsGradientStop {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsGradientStop;
    }
}
export declare class JsMotionGradient {
    constructor(id: string, radial: boolean, x1: number, y1: number, x2: number, y2: number, stops: Array<JsGradientStop>);
    get id(): string;
    get radial(): boolean;
    get x1(): number;
    get y1(): number;
    get x2(): number;
    get y2(): number;
    get stops(): Array<JsGradientStop>;
    copy(id?: string, radial?: boolean, x1?: number, y1?: number, x2?: number, y2?: number, stops?: Array<JsGradientStop>): JsMotionGradient;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsMotionGradient {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsMotionGradient;
    }
}
export declare class JsVeinPath {
    constructor(d: string, layer: string, color: string, width: number, d0: number, len: number, swell: boolean);
    get d(): string;
    get layer(): string;
    get color(): string;
    get width(): number;
    get d0(): number;
    get len(): number;
    get swell(): boolean;
    copy(d?: string, layer?: string, color?: string, width?: number, d0?: number, len?: number, swell?: boolean): JsVeinPath;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsVeinPath {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsVeinPath;
    }
}
export declare class JsVeinNode {
    constructor(x: number, y: number, r: number, d0: number);
    get x(): number;
    get y(): number;
    get r(): number;
    get d0(): number;
    copy(x?: number, y?: number, r?: number, d0?: number): JsVeinNode;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsVeinNode {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsVeinNode;
    }
}
export declare class JsMotionParticle {
    constructor(ox: number, oy: number, dx: number, dy: number, r0: number, life: number, alpha: number);
    get ox(): number;
    get oy(): number;
    get dx(): number;
    get dy(): number;
    get r0(): number;
    get life(): number;
    get alpha(): number;
    copy(ox?: number, oy?: number, dx?: number, dy?: number, r0?: number, life?: number, alpha?: number): JsMotionParticle;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsMotionParticle {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsMotionParticle;
    }
}
export declare class JsMotionPose {
    constructor(x: number, y: number, rotation: number, scaleX: number, scaleY: number);
    get x(): number;
    get y(): number;
    get rotation(): number;
    get scaleX(): number;
    get scaleY(): number;
    copy(x?: number, y?: number, rotation?: number, scaleX?: number, scaleY?: number): JsMotionPose;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsMotionPose {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsMotionPose;
    }
}
export declare class JsMotionGhost {
    constructor(opacity: number, pose: JsMotionPose);
    get opacity(): number;
    get pose(): JsMotionPose;
    copy(opacity?: number, pose?: JsMotionPose): JsMotionGhost;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsMotionGhost {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsMotionGhost;
    }
}
export declare class JsMotionCircle {
    constructor(opacity: number, radius: number, strokeWidth: number);
    get opacity(): number;
    get radius(): number;
    get strokeWidth(): number;
    copy(opacity?: number, radius?: number, strokeWidth?: number): JsMotionCircle;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsMotionCircle {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsMotionCircle;
    }
}
export declare class JsDustState {
    constructor(cx: number, cy: number, r: number, opacity: number);
    get cx(): number;
    get cy(): number;
    get r(): number;
    get opacity(): number;
    copy(cx?: number, cy?: number, r?: number, opacity?: number): JsDustState;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsDustState {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsDustState;
    }
}
export declare class JsSparkState {
    constructor(x1: number, y1: number, x2: number, y2: number, opacity: number);
    get x1(): number;
    get y1(): number;
    get x2(): number;
    get y2(): number;
    get opacity(): number;
    copy(x1?: number, y1?: number, x2?: number, y2?: number, opacity?: number): JsSparkState;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsSparkState {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsSparkState;
    }
}
export declare class JsDropFrame {
    constructor(hitX: number, hitY: number, shadowOpacity: number, shadowRx: number, shadowRy: number, shadowCx: number, shadowCy: number, hammer: Nullable<JsMotionPose>, ghosts: Array<JsMotionGhost>, runeOpacity: number, revealRadius: number, revealFull: boolean, shakeX: number, shakeY: number, shakeRotation: number, shakeScale: number, deviceShakeY: number, stampScale: number, stampOpacity: number, flashOpacity: number, burstOpacity: number, burstScale: number, ring1: JsMotionCircle, ring2: JsMotionCircle, cracksOpacity: number, crackDrawn: number, dust: Array<JsDustState>, sparks: Array<JsSparkState>);
    get hitX(): number;
    get hitY(): number;
    get shadowOpacity(): number;
    get shadowRx(): number;
    get shadowRy(): number;
    get shadowCx(): number;
    get shadowCy(): number;
    get hammer(): Nullable<JsMotionPose>;
    get ghosts(): Array<JsMotionGhost>;
    get runeOpacity(): number;
    get revealRadius(): number;
    get revealFull(): boolean;
    get shakeX(): number;
    get shakeY(): number;
    get shakeRotation(): number;
    get shakeScale(): number;
    get deviceShakeY(): number;
    get stampScale(): number;
    get stampOpacity(): number;
    get flashOpacity(): number;
    get burstOpacity(): number;
    get burstScale(): number;
    get ring1(): JsMotionCircle;
    get ring2(): JsMotionCircle;
    get cracksOpacity(): number;
    get crackDrawn(): number;
    get dust(): Array<JsDustState>;
    get sparks(): Array<JsSparkState>;
    copy(hitX?: number, hitY?: number, shadowOpacity?: number, shadowRx?: number, shadowRy?: number, shadowCx?: number, shadowCy?: number, hammer?: Nullable<JsMotionPose>, ghosts?: Array<JsMotionGhost>, runeOpacity?: number, revealRadius?: number, revealFull?: boolean, shakeX?: number, shakeY?: number, shakeRotation?: number, shakeScale?: number, deviceShakeY?: number, stampScale?: number, stampOpacity?: number, flashOpacity?: number, burstOpacity?: number, burstScale?: number, ring1?: JsMotionCircle, ring2?: JsMotionCircle, cracksOpacity?: number, crackDrawn?: number, dust?: Array<JsDustState>, sparks?: Array<JsSparkState>): JsDropFrame;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsDropFrame {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsDropFrame;
    }
}
export declare class JsPumpFrame {
    constructor(srcX: number, srcY: number, swell: number, contentScale: number, deviceScale: number, glow: number, tintOpacity: number, halos: Array<JsMotionCircle>, rings: Array<JsMotionCircle>, veinsOpacity: number, glowLayerOpacity: number, veinFront: number, veinWidthScale: number, nodeOpacity: number, nodeScale: number, pulseOpacity: number, floodRadius: number, floodFull: boolean, edgeOpacity: number, edgeWidth: number, edgeSoftOpacity: number, edgeSoftWidth: number, pillScale: number);
    get srcX(): number;
    get srcY(): number;
    get swell(): number;
    get contentScale(): number;
    get deviceScale(): number;
    get glow(): number;
    get tintOpacity(): number;
    get halos(): Array<JsMotionCircle>;
    get rings(): Array<JsMotionCircle>;
    get veinsOpacity(): number;
    get glowLayerOpacity(): number;
    get veinFront(): number;
    get veinWidthScale(): number;
    get nodeOpacity(): number;
    get nodeScale(): number;
    get pulseOpacity(): number;
    get floodRadius(): number;
    get floodFull(): boolean;
    get edgeOpacity(): number;
    get edgeWidth(): number;
    get edgeSoftOpacity(): number;
    get edgeSoftWidth(): number;
    get pillScale(): number;
    copy(srcX?: number, srcY?: number, swell?: number, contentScale?: number, deviceScale?: number, glow?: number, tintOpacity?: number, halos?: Array<JsMotionCircle>, rings?: Array<JsMotionCircle>, veinsOpacity?: number, glowLayerOpacity?: number, veinFront?: number, veinWidthScale?: number, nodeOpacity?: number, nodeScale?: number, pulseOpacity?: number, floodRadius?: number, floodFull?: boolean, edgeOpacity?: number, edgeWidth?: number, edgeSoftOpacity?: number, edgeSoftWidth?: number, pillScale?: number): JsPumpFrame;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsPumpFrame {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsPumpFrame;
    }
}
export declare class JsModeSwitchColors {
    constructor(shadow: string, crackShadow: string, crack: string, crackBranch: string, flash: string, flashLight: string, ring1: string, ring2: string, burst: string, spark: string, tint: string, pumpRing: string, edge: string, edgeSoft: string, veinGlow: string, veinCore: string, veinHot: string, veinPulse: string, node: string);
    get shadow(): string;
    get crackShadow(): string;
    get crack(): string;
    get crackBranch(): string;
    get flash(): string;
    get flashLight(): string;
    get ring1(): string;
    get ring2(): string;
    get burst(): string;
    get spark(): string;
    get tint(): string;
    get pumpRing(): string;
    get edge(): string;
    get edgeSoft(): string;
    get veinGlow(): string;
    get veinCore(): string;
    get veinHot(): string;
    get veinPulse(): string;
    get node(): string;
    copy(shadow?: string, crackShadow?: string, crack?: string, crackBranch?: string, flash?: string, flashLight?: string, ring1?: string, ring2?: string, burst?: string, spark?: string, tint?: string, pumpRing?: string, edge?: string, edgeSoft?: string, veinGlow?: string, veinCore?: string, veinHot?: string, veinPulse?: string, node?: string): JsModeSwitchColors;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsModeSwitchColors {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsModeSwitchColors;
    }
}
export declare class JsModeSwitchLightColors {
    constructor(flash: string, flashOpacityScale: number, crackShadowOpacity: number, chalk: Array<JsGradientStop>, veinEdge: string, veinEdgeOpacity: number, ring1: string, spark: string, burstEdge: string, burstEdgeWidth: number);
    get flash(): string;
    get flashOpacityScale(): number;
    get crackShadowOpacity(): number;
    get chalk(): Array<JsGradientStop>;
    get veinEdge(): string;
    get veinEdgeOpacity(): number;
    get ring1(): string;
    get spark(): string;
    get burstEdge(): string;
    get burstEdgeWidth(): number;
    copy(flash?: string, flashOpacityScale?: number, crackShadowOpacity?: number, chalk?: Array<JsGradientStop>, veinEdge?: string, veinEdgeOpacity?: number, ring1?: string, spark?: string, burstEdge?: string, burstEdgeWidth?: number): JsModeSwitchLightColors;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsModeSwitchLightColors {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsModeSwitchLightColors;
    }
}
export declare class JsExercise {
    constructor(id: number, name: string, category: string, parentLift: Nullable<string>, muscles: Array<string>, isBuiltIn: boolean, archived: boolean, plateCount?: string);
    get id(): number;
    get name(): string;
    get category(): string;
    get parentLift(): Nullable<string>;
    get muscles(): Array<string>;
    get isBuiltIn(): boolean;
    get archived(): boolean;
    get plateCount(): string;
    copy(id?: number, name?: string, category?: string, parentLift?: Nullable<string>, muscles?: Array<string>, isBuiltIn?: boolean, archived?: boolean, plateCount?: string): JsExercise;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsExercise {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsExercise;
    }
}
export declare class JsSet {
    constructor(exerciseId: number, weightKg: number, reps: number, rpe: Nullable<number>, failed: boolean, missPoint: Nullable<string>, isTopSet: boolean, unit?: string);
    get exerciseId(): number;
    get weightKg(): number;
    get reps(): number;
    get rpe(): Nullable<number>;
    get failed(): boolean;
    get missPoint(): Nullable<string>;
    get isTopSet(): boolean;
    get unit(): string;
    copy(exerciseId?: number, weightKg?: number, reps?: number, rpe?: Nullable<number>, failed?: boolean, missPoint?: Nullable<string>, isTopSet?: boolean, unit?: string): JsSet;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsSet {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsSet;
    }
}
export declare class JsSession {
    constructor(id: number, date: string, bodyweightKg: Nullable<number>, readiness: Nullable<number>, sets: Array<JsSet>, mode?: string);
    get id(): number;
    get date(): string;
    get bodyweightKg(): Nullable<number>;
    get readiness(): Nullable<number>;
    get sets(): Array<JsSet>;
    get mode(): string;
    copy(id?: number, date?: string, bodyweightKg?: Nullable<number>, readiness?: Nullable<number>, sets?: Array<JsSet>, mode?: string): JsSession;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsSession {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsSession;
    }
}
export declare class JsPlanItem {
    constructor(id: number, weekday: number, exerciseId: number, sets: number, reps: number, targetRpe: number, order: number, mode?: string, repsHigh?: Nullable<number>, supersetWithAbove?: boolean);
    get id(): number;
    get weekday(): number;
    get exerciseId(): number;
    get sets(): number;
    get reps(): number;
    get targetRpe(): number;
    get order(): number;
    get mode(): string;
    get repsHigh(): Nullable<number>;
    get supersetWithAbove(): boolean;
    copy(id?: number, weekday?: number, exerciseId?: number, sets?: number, reps?: number, targetRpe?: number, order?: number, mode?: string, repsHigh?: Nullable<number>, supersetWithAbove?: boolean): JsPlanItem;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsPlanItem {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsPlanItem;
    }
}
export declare class JsSupersetPair {
    constructor(firstId: number, secondId: number);
    get firstId(): number;
    get secondId(): number;
    copy(firstId?: number, secondId?: number): JsSupersetPair;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsSupersetPair {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsSupersetPair;
    }
}
export declare class JsSupersetStep {
    constructor(open: Nullable<string>, rest: boolean);
    get open(): Nullable<string>;
    get rest(): boolean;
    copy(open?: Nullable<string>, rest?: boolean): JsSupersetStep;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsSupersetStep {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsSupersetStep;
    }
}
export declare class JsBaseline {
    constructor(exerciseId: number, e1rmKg: number, date: string);
    get exerciseId(): number;
    get e1rmKg(): number;
    get date(): string;
    copy(exerciseId?: number, e1rmKg?: number, date?: string): JsBaseline;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsBaseline {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsBaseline;
    }
}
export declare class JsAdvice {
    constructor(id: number, type: string, lift: Nullable<string>, createdAt: string, reason: string, severity: number, status: string, payloadJson: string, mode?: string);
    get id(): number;
    get type(): string;
    get lift(): Nullable<string>;
    get createdAt(): string;
    get reason(): string;
    get severity(): number;
    get status(): string;
    get payloadJson(): string;
    get mode(): string;
    copy(id?: number, type?: string, lift?: Nullable<string>, createdAt?: string, reason?: string, severity?: number, status?: string, payloadJson?: string, mode?: string): JsAdvice;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsAdvice {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsAdvice;
    }
}
export declare class JsSkip {
    constructor(id: number, date: string, mode: string, exerciseId: number, planItemId: Nullable<number>, reason: Nullable<string>);
    get id(): number;
    get date(): string;
    get mode(): string;
    get exerciseId(): number;
    get planItemId(): Nullable<number>;
    get reason(): Nullable<string>;
    copy(id?: number, date?: string, mode?: string, exerciseId?: number, planItemId?: Nullable<number>, reason?: Nullable<string>): JsSkip;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsSkip {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsSkip;
    }
}
export declare class JsHistory {
    constructor(sessions: Array<JsSession>, exercises: Array<JsExercise>, baselines: Array<JsBaseline>);
    get sessions(): Array<JsSession>;
    get exercises(): Array<JsExercise>;
    get baselines(): Array<JsBaseline>;
    copy(sessions?: Array<JsSession>, exercises?: Array<JsExercise>, baselines?: Array<JsBaseline>): JsHistory;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsHistory {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsHistory;
    }
}
export declare class JsLastTime {
    constructor(exerciseId: number, date: string, sets: Array<JsSet>);
    get exerciseId(): number;
    get date(): string;
    get sets(): Array<JsSet>;
    copy(exerciseId?: number, date?: string, sets?: Array<JsSet>): JsLastTime;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsLastTime {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsLastTime;
    }
}
export declare class JsWarmUpSet {
    constructor(weight: number, reps: number, unit: string, perSide: Nullable<Array<number>>);
    get weight(): number;
    get reps(): number;
    get unit(): string;
    get perSide(): Nullable<Array<number>>;
    copy(weight?: number, reps?: number, unit?: string, perSide?: Nullable<Array<number>>): JsWarmUpSet;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsWarmUpSet {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsWarmUpSet;
    }
}
export declare class JsBestSet {
    constructor(date: string, set: JsSet);
    get date(): string;
    get set(): JsSet;
    copy(date?: string, set?: JsSet): JsBestSet;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsBestSet {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsBestSet;
    }
}
export declare class JsExerciseTrend {
    constructor(exerciseId: number, name: string, bestSets: Array<JsBestSet>, goingUp: Nullable<boolean>);
    get exerciseId(): number;
    get name(): string;
    get bestSets(): Array<JsBestSet>;
    get goingUp(): Nullable<boolean>;
    copy(exerciseId?: number, name?: string, bestSets?: Array<JsBestSet>, goingUp?: Nullable<boolean>): JsExerciseTrend;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsExerciseTrend {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsExerciseTrend;
    }
}
export declare class JsCoachSettings {
    constructor(defaultRpe: number, changePlates: boolean);
    get defaultRpe(): number;
    get changePlates(): boolean;
    copy(defaultRpe?: number, changePlates?: boolean): JsCoachSettings;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsCoachSettings {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsCoachSettings;
    }
}
export declare class JsWeeklyPoint {
    constructor(weekStart: string, value: number);
    get weekStart(): string;
    get value(): number;
    copy(weekStart?: string, value?: number): JsWeeklyPoint;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsWeeklyPoint {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsWeeklyPoint;
    }
}
export declare class JsPr {
    constructor(date: string, e1rmKg: number, weightKg: number, reps: number, rpe: Nullable<number>);
    get date(): string;
    get e1rmKg(): number;
    get weightKg(): number;
    get reps(): number;
    get rpe(): Nullable<number>;
    copy(date?: string, e1rmKg?: number, weightKg?: number, reps?: number, rpe?: Nullable<number>): JsPr;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsPr {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsPr;
    }
}
export declare class JsAccessoryOption {
    constructor(name: string, heavy: boolean);
    get name(): string;
    get heavy(): boolean;
    copy(name?: string, heavy?: boolean): JsAccessoryOption;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsAccessoryOption {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsAccessoryOption;
    }
}
export declare class JsRatioRange {
    constructor(numerator: string, denominator: string, low: number, high: number);
    get numerator(): string;
    get denominator(): string;
    get low(): number;
    get high(): number;
    copy(numerator?: string, denominator?: string, low?: number, high?: number): JsRatioRange;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsRatioRange {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsRatioRange;
    }
}
export declare class JsPrescription {
    constructor(exerciseName: string, sets: number, repsLow: number, repsHigh: number);
    get exerciseName(): string;
    get sets(): number;
    get repsLow(): number;
    get repsHigh(): number;
    copy(exerciseName?: string, sets?: number, repsLow?: number, repsHigh?: number): JsPrescription;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsPrescription {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsPrescription;
    }
}
export declare class JsSetAddition {
    constructor(weekday: number, exerciseId: number, name: string, fromSets: number, toSets: number);
    get weekday(): number;
    get exerciseId(): number;
    get name(): string;
    get fromSets(): number;
    get toSets(): number;
    copy(weekday?: number, exerciseId?: number, name?: string, fromSets?: number, toSets?: number): JsSetAddition;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsSetAddition {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsSetAddition;
    }
}
export declare class JsNewExercise {
    constructor(weekday: number, name: string, sets: number, repsLow: number, repsHigh: number, rpe: number);
    get weekday(): number;
    get name(): string;
    get sets(): number;
    get repsLow(): number;
    get repsHigh(): number;
    get rpe(): number;
    copy(weekday?: number, name?: string, sets?: number, repsLow?: number, repsHigh?: number, rpe?: number): JsNewExercise;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsNewExercise {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsNewExercise;
    }
}
export declare class JsAdvicePayload {
    constructor(kind: string, exerciseId?: Nullable<number>, weightKg?: Nullable<number>, reps?: Nullable<number>, rpe?: Nullable<number>, sets?: Nullable<number>, items?: Array<JsPrescription>, volumeFactor?: Nullable<number>, loadFactor?: Nullable<number>, lift?: Nullable<string>, missPoint?: Nullable<string>, repsHigh?: Nullable<number>, group?: Nullable<string>, addSets?: Array<JsSetAddition>, newExercise?: Nullable<JsNewExercise>, weeklyBefore?: Nullable<number>, weeklyAfter?: Nullable<number>, name?: Nullable<string>, since?: Nullable<string>, alternatives?: Array<string>, skipId?: Nullable<number>, skipReason?: Nullable<string>, unit?: Nullable<string>);
    get kind(): string;
    get exerciseId(): Nullable<number>;
    get weightKg(): Nullable<number>;
    get reps(): Nullable<number>;
    get rpe(): Nullable<number>;
    get sets(): Nullable<number>;
    get items(): Array<JsPrescription>;
    get volumeFactor(): Nullable<number>;
    get loadFactor(): Nullable<number>;
    get lift(): Nullable<string>;
    get missPoint(): Nullable<string>;
    get repsHigh(): Nullable<number>;
    get group(): Nullable<string>;
    get addSets(): Array<JsSetAddition>;
    get newExercise(): Nullable<JsNewExercise>;
    get weeklyBefore(): Nullable<number>;
    get weeklyAfter(): Nullable<number>;
    get name(): Nullable<string>;
    get since(): Nullable<string>;
    get alternatives(): Array<string>;
    get skipId(): Nullable<number>;
    get skipReason(): Nullable<string>;
    get unit(): Nullable<string>;
    copy(kind?: string, exerciseId?: Nullable<number>, weightKg?: Nullable<number>, reps?: Nullable<number>, rpe?: Nullable<number>, sets?: Nullable<number>, items?: Array<JsPrescription>, volumeFactor?: Nullable<number>, loadFactor?: Nullable<number>, lift?: Nullable<string>, missPoint?: Nullable<string>, repsHigh?: Nullable<number>, group?: Nullable<string>, addSets?: Array<JsSetAddition>, newExercise?: Nullable<JsNewExercise>, weeklyBefore?: Nullable<number>, weeklyAfter?: Nullable<number>, name?: Nullable<string>, since?: Nullable<string>, alternatives?: Array<string>, skipId?: Nullable<number>, skipReason?: Nullable<string>, unit?: Nullable<string>): JsAdvicePayload;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsAdvicePayload {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsAdvicePayload;
    }
}
export declare class JsCheckIn {
    constructor(date: string, lagging: Array<string>, growingWell: Array<string>, id?: number);
    get date(): string;
    get lagging(): Array<string>;
    get growingWell(): Array<string>;
    get id(): number;
    copy(date?: string, lagging?: Array<string>, growingWell?: Array<string>, id?: number): JsCheckIn;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsCheckIn {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsCheckIn;
    }
}
export declare class JsGroupNote {
    constructor(group: string, reason: string, weeklySets: number);
    get group(): string;
    get reason(): string;
    get weeklySets(): number;
    copy(group?: string, reason?: string, weeklySets?: number): JsGroupNote;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsGroupNote {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsGroupNote;
    }
}
export declare class JsCheckInOutcome {
    constructor(advice: Array<JsAdvice>, notes: Array<JsGroupNote>, dismissed: Array<JsAdvice>);
    get advice(): Array<JsAdvice>;
    get notes(): Array<JsGroupNote>;
    get dismissed(): Array<JsAdvice>;
    copy(advice?: Array<JsAdvice>, notes?: Array<JsGroupNote>, dismissed?: Array<JsAdvice>): JsCheckInOutcome;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsCheckInOutcome {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsCheckInOutcome;
    }
}
export declare class JsAddedPlanItem {
    constructor(item: JsPlanItem, name: string);
    get item(): JsPlanItem;
    get name(): string;
    copy(item?: JsPlanItem, name?: string): JsAddedPlanItem;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsAddedPlanItem {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsAddedPlanItem;
    }
}
export declare class JsPlanChange {
    constructor(updated: Array<JsPlanItem>, removed: Array<JsPlanItem>, added: Array<JsAddedPlanItem>, message: string, changed: boolean);
    get updated(): Array<JsPlanItem>;
    get removed(): Array<JsPlanItem>;
    get added(): Array<JsAddedPlanItem>;
    get message(): string;
    get changed(): boolean;
    copy(updated?: Array<JsPlanItem>, removed?: Array<JsPlanItem>, added?: Array<JsAddedPlanItem>, message?: string, changed?: boolean): JsPlanChange;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsPlanChange {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsPlanChange;
    }
}
export declare class JsRating {
    constructor(date: string, rating: string);
    get date(): string;
    get rating(): string;
    copy(date?: string, rating?: string): JsRating;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsRating {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsRating;
    }
}
export declare class JsMuscleRow {
    constructor(group: string, ratings: Array<JsRating>, setsDone: number, setsPlanned: number, exercisesUp: number, exercisesTracked: number);
    get group(): string;
    get ratings(): Array<JsRating>;
    get setsDone(): number;
    get setsPlanned(): number;
    get exercisesUp(): number;
    get exercisesTracked(): number;
    copy(group?: string, ratings?: Array<JsRating>, setsDone?: number, setsPlanned?: number, exercisesUp?: number, exercisesTracked?: number): JsMuscleRow;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsMuscleRow {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsMuscleRow;
    }
}
export declare class JsFormGuide {
    constructor(purpose: string, setup: Array<string>, steps: Array<string>, cues: Array<string>, mistakes: Array<string>);
    get purpose(): string;
    get setup(): Array<string>;
    get steps(): Array<string>;
    get cues(): Array<string>;
    get mistakes(): Array<string>;
    copy(purpose?: string, setup?: Array<string>, steps?: Array<string>, cues?: Array<string>, mistakes?: Array<string>): JsFormGuide;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsFormGuide {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsFormGuide;
    }
}
export declare class JsMuscleFocus {
    constructor(stretch: Nullable<string>, squeeze: Nullable<string>, none: Nullable<string>);
    get stretch(): Nullable<string>;
    get squeeze(): Nullable<string>;
    get none(): Nullable<string>;
    copy(stretch?: Nullable<string>, squeeze?: Nullable<string>, none?: Nullable<string>): JsMuscleFocus;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsMuscleFocus {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsMuscleFocus;
    }
}
export declare class JsRpeGuide {
    constructor(title: string, lines: Array<string>);
    get title(): string;
    get lines(): Array<string>;
    copy(title?: string, lines?: Array<string>): JsRpeGuide;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsRpeGuide {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsRpeGuide;
    }
}
export declare class JsLibraryExercise {
    constructor(name: string, aliases: Array<string>, area: string, equipment: Array<string>, muscles: Array<string>, kind: string, category: string, parentLift: Nullable<string>, guide: JsFormGuide);
    get name(): string;
    get aliases(): Array<string>;
    get area(): string;
    get equipment(): Array<string>;
    get muscles(): Array<string>;
    get kind(): string;
    get category(): string;
    get parentLift(): Nullable<string>;
    get guide(): JsFormGuide;
    copy(name?: string, aliases?: Array<string>, area?: string, equipment?: Array<string>, muscles?: Array<string>, kind?: string, category?: string, parentLift?: Nullable<string>, guide?: JsFormGuide): JsLibraryExercise;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsLibraryExercise {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsLibraryExercise;
    }
}
export declare class JsBuilderPick {
    constructor(name: string, muscles: Array<string>, compound: boolean, equipment: Array<string>, inCatalogue: boolean);
    get name(): string;
    get muscles(): Array<string>;
    get compound(): boolean;
    get equipment(): Array<string>;
    get inCatalogue(): boolean;
    copy(name?: string, muscles?: Array<string>, compound?: boolean, equipment?: Array<string>, inCatalogue?: boolean): JsBuilderPick;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsBuilderPick {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsBuilderPick;
    }
}
export declare class JsDraftExercise {
    constructor(name: string, inCatalogue: boolean, sets: number, repsLow: number, repsHigh: number, rpe: number, mainGroup: Nullable<string>, muscles: Array<string>, compound: boolean, slot: Nullable<string>, lead: boolean);
    get name(): string;
    get inCatalogue(): boolean;
    get sets(): number;
    get repsLow(): number;
    get repsHigh(): number;
    get rpe(): number;
    get mainGroup(): Nullable<string>;
    get muscles(): Array<string>;
    get compound(): boolean;
    get slot(): Nullable<string>;
    get lead(): boolean;
    copy(name?: string, inCatalogue?: boolean, sets?: number, repsLow?: number, repsHigh?: number, rpe?: number, mainGroup?: Nullable<string>, muscles?: Array<string>, compound?: boolean, slot?: Nullable<string>, lead?: boolean): JsDraftExercise;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsDraftExercise {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsDraftExercise;
    }
}
export declare class JsDraftSession {
    constructor(day: number, title: string, exercises: Array<JsDraftExercise>);
    get day(): number;
    get title(): string;
    get exercises(): Array<JsDraftExercise>;
    copy(day?: number, title?: string, exercises?: Array<JsDraftExercise>): JsDraftSession;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsDraftSession {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsDraftSession;
    }
}
export declare class JsGroupVolume {
    constructor(group: string, weeklySets: number, days: number, short: boolean);
    get group(): string;
    get weeklySets(): number;
    get days(): number;
    get short(): boolean;
    copy(group?: string, weeklySets?: number, days?: number, short?: boolean): JsGroupVolume;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsGroupVolume {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsGroupVolume;
    }
}
export declare class JsLeftOut {
    constructor(name: string, reason: string);
    get name(): string;
    get reason(): string;
    copy(name?: string, reason?: string): JsLeftOut;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsLeftOut {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsLeftOut;
    }
}
export declare class JsPlanDraft {
    constructor(sessions: Array<JsDraftSession>, volume: Array<JsGroupVolume>, leftOut: Array<JsLeftOut>);
    get sessions(): Array<JsDraftSession>;
    get volume(): Array<JsGroupVolume>;
    get leftOut(): Array<JsLeftOut>;
    copy(sessions?: Array<JsDraftSession>, volume?: Array<JsGroupVolume>, leftOut?: Array<JsLeftOut>): JsPlanDraft;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsPlanDraft {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsPlanDraft;
    }
}
export declare class JsTemplateExercise {
    constructor(name: string, sets: number, repsLow: number, repsHigh: Nullable<number>, rpe: number, onlyWithStrictPress: boolean);
    get name(): string;
    get sets(): number;
    get repsLow(): number;
    get repsHigh(): Nullable<number>;
    get rpe(): number;
    get onlyWithStrictPress(): boolean;
    copy(name?: string, sets?: number, repsLow?: number, repsHigh?: Nullable<number>, rpe?: number, onlyWithStrictPress?: boolean): JsTemplateExercise;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsTemplateExercise {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsTemplateExercise;
    }
}
export declare class JsTemplateSession {
    constructor(title: string, exercises: Array<JsTemplateExercise>);
    get title(): string;
    get exercises(): Array<JsTemplateExercise>;
    copy(title?: string, exercises?: Array<JsTemplateExercise>): JsTemplateSession;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsTemplateSession {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsTemplateSession;
    }
}
export declare class JsPlanTemplate {
    constructor(id: string, mode: string, name: string, suits: string, dayCount: number, sessions: Array<JsTemplateSession>);
    get id(): string;
    get mode(): string;
    get name(): string;
    get suits(): string;
    get dayCount(): number;
    get sessions(): Array<JsTemplateSession>;
    copy(id?: string, mode?: string, name?: string, suits?: string, dayCount?: number, sessions?: Array<JsTemplateSession>): JsPlanTemplate;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsPlanTemplate {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsPlanTemplate;
    }
}
export declare class JsFigureInfo {
    constructor(startCaption: string, endCaption: string, thumbnail: string, viewBox: Array<number>);
    get startCaption(): string;
    get endCaption(): string;
    get thumbnail(): string;
    get viewBox(): Array<number>;
    copy(startCaption?: string, endCaption?: string, thumbnail?: string, viewBox?: Array<number>): JsFigureInfo;
    toString(): string;
    hashCode(): number;
    equals(other: Nullable<any>): boolean;
}
export declare namespace JsFigureInfo {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsFigureInfo;
    }
}
export declare abstract class JsFigureShape {
    private constructor();
    get kind(): string;
    get role(): string;
}
export declare namespace JsFigureShape {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsFigureShape;
    }
}
export declare class JsFigureCircle extends JsFigureShape.$metadata$.constructor {
    constructor(role: string, cx: number, cy: number, r: number);
    get cx(): number;
    get cy(): number;
    get r(): number;
}
export declare namespace JsFigureCircle {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsFigureCircle;
    }
}
export declare class JsFigureCapsule extends JsFigureShape.$metadata$.constructor {
    constructor(role: string, x1: number, y1: number, x2: number, y2: number, width: number);
    get x1(): number;
    get y1(): number;
    get x2(): number;
    get y2(): number;
    get width(): number;
}
export declare namespace JsFigureCapsule {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsFigureCapsule;
    }
}
export declare class JsFigureRect extends JsFigureShape.$metadata$.constructor {
    constructor(role: string, x: number, y: number, w: number, h: number, radius: number);
    get x(): number;
    get y(): number;
    get w(): number;
    get h(): number;
    get radius(): number;
}
export declare namespace JsFigureRect {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsFigureRect;
    }
}
export declare class JsFigurePolygon extends JsFigureShape.$metadata$.constructor {
    constructor(role: string, points: Array<number>);
    get points(): Array<number>;
}
export declare namespace JsFigurePolygon {
    /** @deprecated $metadata$ is used for internal purposes, please don't use it in your code, because it can be removed at any moment */
    namespace $metadata$ {
        const constructor: abstract new () => JsFigurePolygon;
    }
}
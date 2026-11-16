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
            parseBaselineKg(text: string): Nullable<number>;
            targetsFor(weekday: number, history: JsHistory, plan: Array<JsPlanItem>, settings: JsCoachSettings, today: string, prior: Array<JsAdvice>): Array<JsAdvice>;
            evaluate(history: JsHistory, plan: Array<JsPlanItem>, settings: JsCoachSettings, today: string, prior: Array<JsAdvice>): Array<JsAdvice>;
            coachingTargetsFor(mode: string, weekday: number, history: JsHistory, plan: Array<JsPlanItem>, settings: JsCoachSettings, today: string, prior: Array<JsAdvice>): Array<JsAdvice>;
            coachingEvaluate(mode: string, history: JsHistory, plan: Array<JsPlanItem>, settings: JsCoachSettings, today: string, prior: Array<JsAdvice>): Array<JsAdvice>;
            coachingEvaluateWithCheckIns(mode: string, history: JsHistory, plan: Array<JsPlanItem>, settings: JsCoachSettings, today: string, prior: Array<JsAdvice>, checkIns: Array<JsCheckIn>): Array<JsAdvice>;
            coachingCheckInDue(history: JsHistory, plan: Array<JsPlanItem>, checkIns: Array<JsCheckIn>, today: string): boolean;
            physiqueAfterCheckIn(checkIn: JsCheckIn, history: JsHistory, plan: Array<JsPlanItem>, today: string, prior: Array<JsAdvice>): JsCheckInOutcome;
            physiqueStuck(history: JsHistory, plan: Array<JsPlanItem>, checkIns: Array<JsCheckIn>, prior: Array<JsAdvice>, today: string): Array<JsAdvice>;
            physiqueApplyMoreWork(payloadJson: string, plan: Array<JsPlanItem>, exercises: Array<JsExercise>): JsPlanChange;
            physiqueApplySwap(payloadJson: string, choice: string, plan: Array<JsPlanItem>, exercises: Array<JsExercise>): JsPlanChange;
            physiqueWeeklySets(plan: Array<JsPlanItem>, exercises: Array<JsExercise>, group: string): number;
            muscleProgressRows(history: JsHistory, plan: Array<JsPlanItem>, checkIns: Array<JsCheckIn>, today: string): Array<JsMuscleRow>;
            welcomeBack(leftOn: Nullable<string>, today: string, hasPlan: boolean): boolean;
            repsToBeat(item: JsPlanItem, history: JsHistory, setNumber: number, today: string): number;
            bodybuildingProgress(history: JsHistory, plan: Array<JsPlanItem>, today: string): Array<JsExerciseTrend>;
            planBuilderBuild(days: Array<number>, picks: Array<JsBuilderPick>, priorities: Array<string>, variant: number): JsPlanDraft;
            planBuilderAlternatives(name: string, draft: JsPlanDraft, picks: Array<JsBuilderPick>, day: Nullable<number>): Array<JsLibraryExercise>;
            planBuilderSwap(draft: JsPlanDraft, day: number, from: string, to: string): JsPlanDraft;
            planBuilderSuggestedPicks(history: JsHistory, plans: Array<JsPlanItem>, today: string): Array<number>;
            planBuilderMaxPriorities(): number;
            planBuilderTargetGroups(): Array<string>;
            weeklyTrend(history: JsHistory, lift: string, until: string): Array<JsWeeklyPoint>;
            currentE1rm(history: JsHistory, lift: string, until: string, days: number): Nullable<number>;
            prs(history: JsHistory, lift: string): Array<JsPr>;
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
            private constructor();
        }
    }
}
export declare class JsExercise {
    constructor(id: number, name: string, category: string, parentLift: Nullable<string>, muscles: Array<string>, isBuiltIn: boolean, archived: boolean);
    get id(): number;
    get name(): string;
    get category(): string;
    get parentLift(): Nullable<string>;
    get muscles(): Array<string>;
    get isBuiltIn(): boolean;
    get archived(): boolean;
    copy(id?: number, name?: string, category?: string, parentLift?: Nullable<string>, muscles?: Array<string>, isBuiltIn?: boolean, archived?: boolean): JsExercise;
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
    constructor(exerciseId: number, weightKg: number, reps: number, rpe: Nullable<number>, failed: boolean, missPoint: Nullable<string>, isTopSet: boolean);
    get exerciseId(): number;
    get weightKg(): number;
    get reps(): number;
    get rpe(): Nullable<number>;
    get failed(): boolean;
    get missPoint(): Nullable<string>;
    get isTopSet(): boolean;
    copy(exerciseId?: number, weightKg?: number, reps?: number, rpe?: Nullable<number>, failed?: boolean, missPoint?: Nullable<string>, isTopSet?: boolean): JsSet;
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
    constructor(id: number, weekday: number, exerciseId: number, sets: number, reps: number, targetRpe: number, order: number, mode?: string, repsHigh?: Nullable<number>);
    get id(): number;
    get weekday(): number;
    get exerciseId(): number;
    get sets(): number;
    get reps(): number;
    get targetRpe(): number;
    get order(): number;
    get mode(): string;
    get repsHigh(): Nullable<number>;
    copy(id?: number, weekday?: number, exerciseId?: number, sets?: number, reps?: number, targetRpe?: number, order?: number, mode?: string, repsHigh?: Nullable<number>): JsPlanItem;
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
    constructor(kind: string, exerciseId?: Nullable<number>, weightKg?: Nullable<number>, reps?: Nullable<number>, rpe?: Nullable<number>, sets?: Nullable<number>, items?: Array<JsPrescription>, volumeFactor?: Nullable<number>, loadFactor?: Nullable<number>, lift?: Nullable<string>, missPoint?: Nullable<string>, repsHigh?: Nullable<number>, group?: Nullable<string>, addSets?: Array<JsSetAddition>, newExercise?: Nullable<JsNewExercise>, weeklyBefore?: Nullable<number>, weeklyAfter?: Nullable<number>, name?: Nullable<string>, since?: Nullable<string>, alternatives?: Array<string>);
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
    copy(kind?: string, exerciseId?: Nullable<number>, weightKg?: Nullable<number>, reps?: Nullable<number>, rpe?: Nullable<number>, sets?: Nullable<number>, items?: Array<JsPrescription>, volumeFactor?: Nullable<number>, loadFactor?: Nullable<number>, lift?: Nullable<string>, missPoint?: Nullable<string>, repsHigh?: Nullable<number>, group?: Nullable<string>, addSets?: Array<JsSetAddition>, newExercise?: Nullable<JsNewExercise>, weeklyBefore?: Nullable<number>, weeklyAfter?: Nullable<number>, name?: Nullable<string>, since?: Nullable<string>, alternatives?: Array<string>): JsAdvicePayload;
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
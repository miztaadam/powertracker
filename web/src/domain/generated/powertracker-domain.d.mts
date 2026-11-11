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
            weeklyTrend(history: JsHistory, lift: string, until: string): Array<JsWeeklyPoint>;
            currentE1rm(history: JsHistory, lift: string, until: string, days: number): Nullable<number>;
            prs(history: JsHistory, lift: string): Array<JsPr>;
            weeklyVolume(history: JsHistory, lift: string): Array<JsWeeklyPoint>;
            accessoryOptions(lift: string, miss: Nullable<string>): Array<JsAccessoryOption>;
            accessoryMuscles(lift: string, miss: Nullable<string>): Array<string>;
            seedExercises(): Array<JsExercise>;
            total(history: JsHistory, until: string): Nullable<number>;
            mainLiftOf(exercise: JsExercise): Nullable<string>;
            isMainLift(exercise: JsExercise): boolean;
            lastTopSet(history: JsHistory, exerciseId: number, before: string): Nullable<JsSet>;
            openAdviceExpired(type: string, createdAt: string, today: string): boolean;
            advicePayload(payloadJson: string): Nullable<JsAdvicePayload>;
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
    constructor(id: number, date: string, bodyweightKg: Nullable<number>, readiness: Nullable<number>, sets: Array<JsSet>);
    get id(): number;
    get date(): string;
    get bodyweightKg(): Nullable<number>;
    get readiness(): Nullable<number>;
    get sets(): Array<JsSet>;
    copy(id?: number, date?: string, bodyweightKg?: Nullable<number>, readiness?: Nullable<number>, sets?: Array<JsSet>): JsSession;
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
    constructor(id: number, weekday: number, exerciseId: number, sets: number, reps: number, targetRpe: number, order: number);
    get id(): number;
    get weekday(): number;
    get exerciseId(): number;
    get sets(): number;
    get reps(): number;
    get targetRpe(): number;
    get order(): number;
    copy(id?: number, weekday?: number, exerciseId?: number, sets?: number, reps?: number, targetRpe?: number, order?: number): JsPlanItem;
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
    constructor(id: number, type: string, lift: Nullable<string>, createdAt: string, reason: string, severity: number, status: string, payloadJson: string);
    get id(): number;
    get type(): string;
    get lift(): Nullable<string>;
    get createdAt(): string;
    get reason(): string;
    get severity(): number;
    get status(): string;
    get payloadJson(): string;
    copy(id?: number, type?: string, lift?: Nullable<string>, createdAt?: string, reason?: string, severity?: number, status?: string, payloadJson?: string): JsAdvice;
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
export declare class JsAdvicePayload {
    constructor(kind: string, exerciseId?: Nullable<number>, weightKg?: Nullable<number>, reps?: Nullable<number>, rpe?: Nullable<number>, sets?: Nullable<number>, items?: Array<JsPrescription>, volumeFactor?: Nullable<number>, loadFactor?: Nullable<number>, lift?: Nullable<string>, missPoint?: Nullable<string>);
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
    copy(kind?: string, exerciseId?: Nullable<number>, weightKg?: Nullable<number>, reps?: Nullable<number>, rpe?: Nullable<number>, sets?: Nullable<number>, items?: Array<JsPrescription>, volumeFactor?: Nullable<number>, loadFactor?: Nullable<number>, lift?: Nullable<string>, missPoint?: Nullable<string>): JsAdvicePayload;
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
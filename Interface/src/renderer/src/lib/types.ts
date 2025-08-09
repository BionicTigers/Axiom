export type BaseResponse<T> = {
    name: string,
    tick: number,
    data: T
}

export type UUID = string
export type BaseResponse = {
    name: string,
    tick: number,
    data: any
}

export type UUID = string

// Command state types used across registry components
export type CommandStateValueBase = string | number | boolean | null
export type CommandStateValue = { value: CommandStateValueBase; readonly: boolean }
export type CommandState = CommandStateValue | CommandState[] | { [key: string]: CommandState }
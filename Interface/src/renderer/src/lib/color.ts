export function getCommandExecutionTimeColor(executionTime: number) {
  if (executionTime < 6) return 'rgb(100, 240, 100)'
  if (executionTime < 12) return 'rgb(240, 240, 100)'
  return 'rgb(240, 100, 100)'
}

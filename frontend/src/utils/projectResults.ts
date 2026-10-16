export type ResultType = 'COST_SAVING' | 'ADDITIONAL_REVENUE' | 'PRODUCTIVITY_GAIN' | 'TIME_REDUCTION'

export interface ResultForm {
  type: ResultType
  nature: 'FORECAST' | 'ACTUAL'
  description: string
  periodStart: string
  periodEnd: string
  unit: string
  baselineValue: number | ''
  achievedValue: number | ''
  financialAmount: number | ''
}

export const isFinancialResult = (type: ResultType) => type === 'COST_SAVING' || type === 'ADDITIONAL_REVENUE'

export const createEmptyResultForm = (): ResultForm => ({
  type: 'COST_SAVING', nature: 'ACTUAL', description: '', periodStart: '', periodEnd: '',
  unit: '', baselineValue: '', achievedValue: '', financialAmount: '',
})

export const validateResultForm = (result: ResultForm) => {
  if (!result.description.trim() || !result.periodStart || !result.periodEnd) return 'Preencha a descrição e o período do resultado.'
  if (result.periodEnd < result.periodStart) return 'O fim do período não pode ser anterior ao início.'
  if (isFinancialResult(result.type)) return result.financialAmount === '' ? 'Informe o valor financeiro.' : ''
  if (!result.unit.trim() || result.baselineValue === '' || result.achievedValue === '') return 'Informe a unidade, o valor inicial e o valor alcançado.'
  return ''
}

export const buildResultPayload = (result: ResultForm) => {
  const common = { type: result.type, nature: result.nature, description: result.description.trim(), periodStart: result.periodStart, periodEnd: result.periodEnd }
  return isFinancialResult(result.type)
    ? { ...common, financialAmount: result.financialAmount }
    : { ...common, unit: result.unit.trim(), baselineValue: result.baselineValue, achievedValue: result.achievedValue }
}

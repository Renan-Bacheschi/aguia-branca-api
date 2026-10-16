import { describe, expect, it } from 'vitest'
import { buildResultPayload, createEmptyResultForm, validateResultForm } from './projectResults'

describe('project result payload', () => {
  it('sends only financial data for a cost saving', () => {
    const result = { ...createEmptyResultForm(), description: 'Renegociação de contrato', periodStart: '2026-01-01', periodEnd: '2026-01-31', unit: 'horas', baselineValue: 10, achievedValue: 12, financialAmount: 1500.5 }
    expect(buildResultPayload(result)).toEqual({ type: 'COST_SAVING', nature: 'ACTUAL', description: 'Renegociação de contrato', periodStart: '2026-01-01', periodEnd: '2026-01-31', financialAmount: 1500.5 })
  })

  it('sends only operational data for a productivity gain', () => {
    const result = { ...createEmptyResultForm(), type: 'PRODUCTIVITY_GAIN' as const, description: 'Separação de pedidos', periodStart: '2026-01-01', periodEnd: '2026-01-31', unit: 'pedidos/hora', baselineValue: 10, achievedValue: 14, financialAmount: 999 }
    expect(buildResultPayload(result)).toEqual({ type: 'PRODUCTIVITY_GAIN', nature: 'ACTUAL', description: 'Separação de pedidos', periodStart: '2026-01-01', periodEnd: '2026-01-31', unit: 'pedidos/hora', baselineValue: 10, achievedValue: 14 })
  })

  it('requires the fields that correspond to the selected result type', () => {
    expect(validateResultForm(createEmptyResultForm())).toBe('Preencha a descrição e o período do resultado.')
    expect(validateResultForm({ ...createEmptyResultForm(), type: 'TIME_REDUCTION', description: 'Tempo de atendimento', periodStart: '2026-01-01', periodEnd: '2026-01-31' })).toBe('Informe a unidade, o valor inicial e o valor alcançado.')
  })
})

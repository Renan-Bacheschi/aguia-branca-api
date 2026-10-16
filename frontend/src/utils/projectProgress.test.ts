import { describe, expect, it } from 'vitest'
import { buildProgressPayload, createProgressForm } from './projectProgress'
import type { Project } from '../types/api'

const project: Project = {
  id: 'project-1', ideaId: 'idea-1', strategyId: 'strategy-1', name: 'Projeto', description: 'Descrição', responsibleName: 'Ana',
  stage: 'MONITORING', status: 'IN_PROGRESS', plannedStartDate: '2026-01-01', plannedEndDate: '2026-12-31', actualEndDate: null,
  plannedInvestment: 10000, actualInvestment: 4200.5, progressPercentage: 65, archived: false,
}

describe('project progress form', () => {
  it('initializes every field from the selected project', () => {
    expect(createProgressForm(project)).toEqual({ stage: 'MONITORING', status: 'IN_PROGRESS', progressPercentage: 65, actualInvestment: 4200.5, actualEndDate: '' })
  })

  it('sends an empty end date as null', () => {
    expect(buildProgressPayload(createProgressForm(project))).toMatchObject({ actualEndDate: null, actualInvestment: 4200.5 })
  })
})

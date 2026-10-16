import type { Project, ProjectStage, ProjectStatus } from '../types/api'

export interface ProgressForm {
  stage: ProjectStage
  status: ProjectStatus
  progressPercentage: number
  actualInvestment: number
  actualEndDate: string
}

export const createProgressForm = (project: Project): ProgressForm => ({
  stage: project.stage,
  status: project.status,
  progressPercentage: project.progressPercentage,
  actualInvestment: project.actualInvestment,
  actualEndDate: project.actualEndDate ?? '',
})

export const buildProgressPayload = (progress: ProgressForm) => ({
  ...progress,
  actualEndDate: progress.actualEndDate || null,
})

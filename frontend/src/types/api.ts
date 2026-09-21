export type Role = 'OPERATOR' | 'MANAGER' | 'LEADER'
export type IdeaStatus = 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'
export type IdeaPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export type ProjectStatus = 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type ProjectStage = 'PLANNING' | 'EXECUTION' | 'CLOSURE'

export interface User { id: string; name: string; email: string; role: Role }
export interface LoginResponse { accessToken: string; tokenType: string; expiresInSeconds: number; user: User }
export interface PageResponse<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
export interface ProblemDetail { title?: string; detail?: string; status?: number; errors?: Record<string, string> }
export interface Strategy { id: string; title: string; description: string; category: string; campaign: string; startsOn: string; endsOn: string; active: boolean; archived: boolean; revision: number }
export interface StrategyHistory { strategyId: string; revision: number; operation: string; title: string; description: string; category: string; campaign: string; startsOn: string; endsOn: string; archived: boolean; changedByUserId: string; changedAt: string }
export interface Idea { id: string; title: string; problem: string; proposedSolution: string; expectedBenefits: string; strategyId: string; authorUserId: string; status: IdeaStatus; priority?: IdeaPriority; reviewJustification?: string; submittedAt?: string; reviewedAt?: string; createdAt: string; updatedAt: string }
export interface Analysis { ideaId: string; strategyId: string; summary: string; strategicAlignment: string; potentialBenefits: string; risks: string; missingInformation: string; recommendation: string; generatedAt: string; provider: string; model: string }
export interface Project { id: string; ideaId: string; strategyId: string; name: string; description: string; responsibleName: string; stage: ProjectStage; status: ProjectStatus; plannedStartDate: string; plannedEndDate: string; actualEndDate?: string; plannedInvestment: number; actualInvestment: number; progressPercentage: number; archived: boolean }
export interface ProjectResult { id: string; projectId: string; type: string; nature: string; description: string; periodStart: string; periodEnd: string; unit?: string; baselineValue?: number; achievedValue?: number; financialAmount?: number }
export interface SummaryReport { totalStrategies: number; activeStrategies: number; archivedStrategies: number; totalIdeas: number; ideasByStatus: Record<string, number>; ideasByPriority: Record<string, number>; totalProjects: number; activeProjectCount: number; archivedProjectCount: number; projectsByStatus: Record<string, number>; projectsByStage: Record<string, number>; totalResults: number; forecastResultCount: number; actualResultCount: number; plannedInvestment: number; actualInvestment: number; forecastFinancialBenefit: number; actualFinancialBenefit: number; forecastNetBenefit: number; actualNetBenefit: number; forecastRoi?: number; forecastRoiAvailable: boolean; actualRoi?: number; actualRoiAvailable: boolean; strategies: StrategyBreakdown[] }
export interface StrategyBreakdown { strategyId: string; strategyTitle: string; category: string; campaign: string; active: boolean; archived: boolean; ideaCount: number; projectCount: number; activeProjectCount: number; actualInvestment: number; actualFinancialBenefit: number; actualNetBenefit: number; actualRoi?: number; actualRoiAvailable: boolean }

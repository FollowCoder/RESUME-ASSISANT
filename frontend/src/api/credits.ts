import apiClient from './client'

export interface TransactionItem {
  id: number
  module: string
  moduleName: string
  creditsUsed: number
  description: string
  createdAt: string
}

export interface BalanceResponse {
  balance: number
}

export const creditsApi = {
  getBalance: () => apiClient.get<any, BalanceResponse>('/credits/balance'),
  purchase: (amount: number) => apiClient.post<any, BalanceResponse>('/credits/purchase', { amount }),
  getTransactions: () => apiClient.get<any, TransactionItem[]>('/credits/transactions')
}

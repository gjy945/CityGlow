import { apiClient } from './client'

export interface AuthUser {
  id: number
  username: string
  avatarUrl: string | null
}

export interface AuthResponse {
  token: string
  user: AuthUser
}

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
}

export const authApi = {
  login: (data: LoginPayload) =>
    apiClient.post<unknown, AuthResponse>('/auth/login', data),
  register: (data: RegisterPayload) =>
    apiClient.post<unknown, AuthResponse>('/auth/register', data),
  me: () => apiClient.get<unknown, AuthUser>('/auth/me'),
}

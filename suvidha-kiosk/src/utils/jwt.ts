export function decodeJwtPayload(token: string): Record<string, unknown> | null {
  const parts = token.split('.')
  if (parts.length !== 3) return null
  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
    const json = atob(padded)
    return JSON.parse(json)
  } catch {
    return null
  }
}

export function tokenHasAdminRole(token: string | null) {
  if (!token) return false
  const payload = decodeJwtPayload(token)
  const role = payload?.['role']
  const roles = payload?.['roles']
  if (typeof role === 'string') return role === 'ADMIN'
  if (Array.isArray(roles)) return roles.includes('ADMIN')
  return false
}

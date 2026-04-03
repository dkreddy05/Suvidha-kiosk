import type { NavigateFunction } from 'react-router-dom'

let _navigate: NavigateFunction | null = null

export function setNavigator(navigate: NavigateFunction) {
  _navigate = navigate
}

export function getNavigator() {
  return _navigate
}

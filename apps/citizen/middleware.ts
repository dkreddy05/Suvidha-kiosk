import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const protectedPathPrefixes = ['/dashboard', '/billing', '/connections', '/grievances', '/profile', '/notifications'];

const publicPaths = ['/login', '/register', '/api'];

/**
 * Lightweight JWT expiry check — decodes the payload without verifying signature.
 * Full validation is handled by the API gateway.
 */
function isJwtExpired(token: string): boolean {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return true;
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const payload = JSON.parse(atob(base64));
    if (!payload.exp) return true;
    // Add 30-second buffer for clock skew
    return payload.exp * 1000 < Date.now() - 30_000;
  } catch {
    return true;
  }
}

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  const isProtected = protectedPathPrefixes.some((prefix) =>
    pathname.startsWith(prefix)
  );

  const isPublic = publicPaths.some(
    (path) => pathname === path || pathname.startsWith(path)
  );

  const isStatic =
    pathname.startsWith('/_next') ||
    pathname.startsWith('/static') ||
    pathname.startsWith('/favicon') ||
    pathname.startsWith('/images') ||
    pathname === '/';

  if (isStatic || isPublic) {
    return NextResponse.next();
  }

  if (isProtected) {
    const sessionCookie = request.cookies.get('session');

    if (!sessionCookie || !sessionCookie.value || isJwtExpired(sessionCookie.value)) {
      const loginUrl = new URL('/login', request.url);
      loginUrl.searchParams.set('redirect', pathname);
      return NextResponse.redirect(loginUrl);
    }
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico|images).*)'],
};

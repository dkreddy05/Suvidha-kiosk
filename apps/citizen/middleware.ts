import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

const protectedPathPrefixes = ['/dashboard', '/billing', '/connections', '/grievances', '/profile', '/notifications'];

const publicPaths = ['/login', '/register', '/api'];

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

    if (!sessionCookie) {
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

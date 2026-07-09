import { NextRequest, NextResponse } from 'next/server';

export async function POST(request: NextRequest) {
  const sessionCookie = request.cookies.get('session');
  const accessToken = sessionCookie?.value;

  const response = NextResponse.json({ success: true });

  // Clear the session cookie
  response.cookies.set({
    name: 'session',
    value: '',
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    path: '/',
    expires: new Date(0),
  });

  if (accessToken) {
    try {
      const gatewayUrl = process.env.GATEWAY_URL || 'http://suvidha-gateway:8080';
      await fetch(`${gatewayUrl}/api/auth/logout`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify({ token: accessToken }),
      });
    } catch (error) {
      console.error('Error invalidating token on logout:', error);
    }
  }

  return response;
}

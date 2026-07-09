import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  const sessionCookie = request.cookies.get('session');
  const accessToken = sessionCookie?.value;

  if (!accessToken) {
    return NextResponse.json({ authenticated: false, error: 'No session' }, { status: 401 });
  }

  try {
    const gatewayUrl = process.env.GATEWAY_URL || 'http://suvidha-gateway:8080';
    const res = await fetch(`${gatewayUrl}/api/auth/profile`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (res.ok) {
      const citizen = await res.json();
      // Return the token alongside the citizen so the client can use it
      // as a Bearer token for direct gateway API calls.
      return NextResponse.json({ ...citizen, accessToken });
    } else {
      return NextResponse.json({ authenticated: false, error: 'Failed to fetch profile' }, { status: res.status });
    }
  } catch (error) {
    console.error('Error in /api/auth/me:', error);
    return NextResponse.json({ authenticated: false, error: 'Internal server error' }, { status: 500 });
  }
}

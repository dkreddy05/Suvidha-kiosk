import { NextRequest, NextResponse } from 'next/server';

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const { accessToken, action } = body;

    if (action === 'check') {
      const sessionCookie = request.cookies.get('session');
      const isValid = !!sessionCookie?.value;
      return NextResponse.json({ authenticated: isValid });
    }

    const response = NextResponse.json({ success: true });

    if (action === 'clear' || !accessToken) {
      response.cookies.set({
        name: 'session',
        value: '',
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'lax',
        path: '/',
        expires: new Date(0),
      });
      return response;
    }

    response.cookies.set({
      name: 'session',
      value: accessToken,
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
      path: '/',
      maxAge: 60 * 30,
    });

    return response;
  } catch (error) {
    return NextResponse.json({ success: false, error: 'Invalid request' }, { status: 400 });
  }
}

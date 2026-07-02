import Link from 'next/link';
import { AuthLayoutClient } from './AuthLayoutClient';

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen">
      <div className="hidden lg:flex lg:w-1/2 relative bg-gradient-to-br from-brand-600 to-brand-900 p-12 flex-col justify-between">
        <div>
          <Link href="/" className="inline-flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/20 backdrop-blur text-white font-bold text-lg">
              S
            </div>
            <span className="text-2xl font-display font-bold text-white">
              Suvidha
            </span>
          </Link>
        </div>
        <div className="space-y-4">
          <blockquote className="text-white/90 text-lg font-display leading-relaxed">
            &ldquo;Access your water services, pay bills, track grievances, and manage connections — all in one place.&rdquo;
          </blockquote>
          <p className="text-white/60 text-sm">
            Suvidha Citizen Portal
          </p>
        </div>
        <div className="text-white/40 text-xs">
          &copy; {new Date().getFullYear()} Suvidha. All rights reserved.
        </div>
      </div>
      <div className="flex-1 flex flex-col">
        <div className="flex justify-end p-4">
          <AuthLayoutClient />
        </div>
        <div className="flex-1 flex items-center justify-center px-4 sm:px-6 lg:px-8">
          <div className="w-full max-w-md">{children}</div>
        </div>
      </div>
    </div>
  );
}

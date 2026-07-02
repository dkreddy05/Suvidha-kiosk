"use client";

import { useState, useMemo, useCallback } from "react";
import { motion } from "framer-motion";
import { toast } from "sonner";
import {
  Users,
  Search,
  Eye,
  ChevronLeft,
  ChevronRight,
  ArrowUpDown,
  RefreshCw,
} from "lucide-react";
import {
  Button,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Badge,
  Input,
  Skeleton,
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  Avatar,
} from "@suvidha/ui";
import { adminApi } from "@/lib/api/client";
import { formatDate } from "@suvidha/ui";
import { maskMobile } from "@suvidha/ui";

interface AdminUser {
  id: string;
  name: string;
  mobile: string;
  email: string;
  role: "ADMIN" | "USER" | "EMPLOYEE";
  status: "ACTIVE" | "INACTIVE";
  joinedAt: string;
  address?: string;
  city?: string;
  state?: string;
  aadhaar?: string;
  totalGrievances?: number;
  lastActive?: string;
}

type SortKey = keyof AdminUser;
type SortDir = "asc" | "desc";

const ROLE_VARIANT: Record<string, "default" | "info" | "warning"> = {
  ADMIN: "default",
  USER: "info",
  EMPLOYEE: "warning",
};

const STATUS_VARIANT: Record<string, "success" | "default"> = {
  ACTIVE: "success",
  INACTIVE: "default",
};

const MOCK_USERS: AdminUser[] = [
  {
    id: "USR-001",
    name: "Rahul Sharma",
    mobile: "9876543210",
    email: "rahul.sharma@example.com",
    role: "USER",
    status: "ACTIVE",
    joinedAt: "2024-01-15T10:30:00Z",
    address: "42, MG Road, Indiranagar",
    city: "Bangalore",
    state: "Karnataka",
    aadhaar: "123456789012",
    totalGrievances: 3,
    lastActive: "2025-05-23T14:22:00Z",
  },
  {
    id: "USR-002",
    name: "Priya Patel",
    mobile: "8765432109",
    email: "priya.patel@example.com",
    role: "ADMIN",
    status: "ACTIVE",
    joinedAt: "2023-08-01T09:00:00Z",
    address: "15, Lake View Apartments",
    city: "Mumbai",
    state: "Maharashtra",
    aadhaar: "234567890123",
    totalGrievances: 0,
    lastActive: "2025-05-24T08:15:00Z",
  },
  {
    id: "USR-003",
    name: "Amit Kumar",
    mobile: "7654321098",
    email: "amit.kumar@example.com",
    role: "USER",
    status: "INACTIVE",
    joinedAt: "2024-03-20T11:45:00Z",
    address: "88, Sector 12",
    city: "Noida",
    state: "Uttar Pradesh",
    aadhaar: "345678901234",
    totalGrievances: 1,
    lastActive: "2025-02-10T16:30:00Z",
  },
  {
    id: "USR-004",
    name: "Sunita Verma",
    mobile: "6543210987",
    email: "sunita.verma@example.com",
    role: "EMPLOYEE",
    status: "ACTIVE",
    joinedAt: "2024-06-10T08:30:00Z",
    address: "7, Green Park Colony",
    city: "Delhi",
    state: "Delhi",
    aadhaar: "456789012345",
    totalGrievances: 5,
    lastActive: "2025-05-22T10:00:00Z",
  },
  {
    id: "USR-005",
    name: "Vijay Singh",
    mobile: "5432109876",
    email: "vijay.singh@example.com",
    role: "USER",
    status: "ACTIVE",
    joinedAt: "2024-09-05T14:00:00Z",
    address: "120, Civil Lines",
    city: "Jaipur",
    state: "Rajasthan",
    aadhaar: "567890123456",
    totalGrievances: 2,
    lastActive: "2025-05-23T18:45:00Z",
  },
  {
    id: "USR-006",
    name: "Neha Gupta",
    mobile: "4321098765",
    email: "neha.gupta@example.com",
    role: "ADMIN",
    status: "ACTIVE",
    joinedAt: "2023-11-12T10:00:00Z",
    address: "55, Ballygunge Circular Road",
    city: "Kolkata",
    state: "West Bengal",
    aadhaar: "678901234567",
    totalGrievances: 0,
    lastActive: "2025-05-24T07:30:00Z",
  },
  {
    id: "USR-007",
    name: "Ravi Deshmukh",
    mobile: "3210987654",
    email: "ravi.deshmukh@example.com",
    role: "EMPLOYEE",
    status: "INACTIVE",
    joinedAt: "2024-04-18T13:15:00Z",
    address: "29, FC Road",
    city: "Pune",
    state: "Maharashtra",
    aadhaar: "789012345678",
    totalGrievances: 8,
    lastActive: "2025-01-05T11:00:00Z",
  },
  {
    id: "USR-008",
    name: "Anjali Mehta",
    mobile: "2109876543",
    email: "anjali.mehta@example.com",
    role: "USER",
    status: "ACTIVE",
    joinedAt: "2025-02-28T09:45:00Z",
    address: "102, Ashram Road",
    city: "Ahmedabad",
    state: "Gujarat",
    aadhaar: "890123456789",
    totalGrievances: 1,
    lastActive: "2025-05-20T15:30:00Z",
  },
  {
    id: "USR-009",
    name: "Deepak Joshi",
    mobile: "1098765432",
    email: "deepak.joshi@example.com",
    role: "USER",
    status: "INACTIVE",
    joinedAt: "2024-07-22T16:00:00Z",
    address: "5, Mall Road",
    city: "Dehradun",
    state: "Uttarakhand",
    aadhaar: "901234567890",
    totalGrievances: 0,
    lastActive: "2024-12-15T09:00:00Z",
  },
  {
    id: "USR-010",
    name: "Kavita Nair",
    mobile: "9988776655",
    email: "kavita.nair@example.com",
    role: "EMPLOYEE",
    status: "ACTIVE",
    joinedAt: "2024-05-14T07:30:00Z",
    address: "33, Marine Drive",
    city: "Kochi",
    state: "Kerala",
    aadhaar: "012345678901",
    totalGrievances: 4,
    lastActive: "2025-05-23T12:00:00Z",
  },
  {
    id: "USR-011",
    name: "Suresh Reddy",
    mobile: "8877665544",
    email: "suresh.reddy@example.com",
    role: "USER",
    status: "ACTIVE",
    joinedAt: "2024-10-01T10:00:00Z",
    address: "12, Jubilee Hills",
    city: "Hyderabad",
    state: "Telangana",
    aadhaar: "112233445566",
    totalGrievances: 2,
    lastActive: "2025-05-19T14:00:00Z",
  },
  {
    id: "USR-012",
    name: "Meera Iyer",
    mobile: "7766554433",
    email: "meera.iyer@example.com",
    role: "ADMIN",
    status: "ACTIVE",
    joinedAt: "2023-09-05T08:00:00Z",
    address: "8, Cathedral Road",
    city: "Chennai",
    state: "Tamil Nadu",
    aadhaar: "223344556677",
    totalGrievances: 0,
    lastActive: "2025-05-24T06:00:00Z",
  },
  {
    id: "USR-013",
    name: "Arun Thakur",
    mobile: "6655443322",
    email: "arun.thakur@example.com",
    role: "EMPLOYEE",
    status: "INACTIVE",
    joinedAt: "2024-02-14T12:30:00Z",
    address: "22, VIP Road",
    city: "Chandigarh",
    state: "Chandigarh",
    aadhaar: "334455667788",
    totalGrievances: 6,
    lastActive: "2025-03-28T10:00:00Z",
  },
  {
    id: "USR-014",
    name: "Pooja Bhat",
    mobile: "5544332211",
    email: "pooja.bhat@example.com",
    role: "USER",
    status: "ACTIVE",
    joinedAt: "2025-01-10T15:00:00Z",
    address: "90, Kalyani Nagar",
    city: "Pune",
    state: "Maharashtra",
    aadhaar: "445566778899",
    totalGrievances: 0,
    lastActive: "2025-05-22T17:30:00Z",
  },
  {
    id: "USR-015",
    name: "Manoj Tiwari",
    mobile: "4433221100",
    email: "manoj.tiwari@example.com",
    role: "USER",
    status: "ACTIVE",
    joinedAt: "2024-12-20T11:00:00Z",
    address: "67, Hazratganj",
    city: "Lucknow",
    state: "Uttar Pradesh",
    aadhaar: "556677889900",
    totalGrievances: 3,
    lastActive: "2025-05-21T09:15:00Z",
  },
];

function UserDetailDialog({
  user,
  open,
  onClose,
}: {
  user: AdminUser | null;
  open: boolean;
  onClose: () => void;
}) {
  if (!user) return null;
  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>User Details</DialogTitle>
          <DialogDescription>Full information for {user.name}</DialogDescription>
        </DialogHeader>
        <div className="space-y-6">
          <div className="flex items-center gap-4">
            <Avatar className="h-16 w-16">
              <div className="flex h-full w-full items-center justify-center rounded-full bg-brand-100 text-lg font-bold text-brand-700 dark:bg-brand-900/30 dark:text-brand-300">
                {user.name
                  .split(" ")
                  .map((n) => n[0])
                  .join("")
                  .toUpperCase()
                  .slice(0, 2)}
              </div>
            </Avatar>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                {user.name}
              </h3>
              <Badge variant={ROLE_VARIANT[user.role]}>
                {user.role}
              </Badge>
              <span className="ml-2">
                <Badge variant={STATUS_VARIANT[user.status]}>
                  {user.status}
                </Badge>
              </span>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="font-medium text-gray-500 dark:text-gray-400">User ID</p>
              <p className="text-gray-900 dark:text-gray-100">{user.id}</p>
            </div>
            <div>
              <p className="font-medium text-gray-500 dark:text-gray-400">Email</p>
              <p className="text-gray-900 dark:text-gray-100">{user.email}</p>
            </div>
            <div>
              <p className="font-medium text-gray-500 dark:text-gray-400">Mobile</p>
              <p className="text-gray-900 dark:text-gray-100">{user.mobile}</p>
            </div>
            <div>
              <p className="font-medium text-gray-500 dark:text-gray-400">Aadhaar</p>
              <p className="text-gray-900 dark:text-gray-100">•••• •••• {user.aadhaar?.slice(-4)}</p>
            </div>
            <div>
              <p className="font-medium text-gray-500 dark:text-gray-400">Address</p>
              <p className="text-gray-900 dark:text-gray-100">{user.address}, {user.city}</p>
            </div>
            <div>
              <p className="font-medium text-gray-500 dark:text-gray-400">State</p>
              <p className="text-gray-900 dark:text-gray-100">{user.state}</p>
            </div>
            <div>
              <p className="font-medium text-gray-500 dark:text-gray-400">Joined</p>
              <p className="text-gray-900 dark:text-gray-100">{formatDate(user.joinedAt, "long")}</p>
            </div>
            <div>
              <p className="font-medium text-gray-500 dark:text-gray-400">Last Active</p>
              <p className="text-gray-900 dark:text-gray-100">
                {user.lastActive ? formatDate(user.lastActive, "relative") : "—"}
              </p>
            </div>
            <div>
              <p className="font-medium text-gray-500 dark:text-gray-400">Total Grievances</p>
              <p className="text-gray-900 dark:text-gray-100">{user.totalGrievances ?? 0}</p>
            </div>
            <div>
              <p className="font-medium text-gray-500 dark:text-gray-400">Role</p>
              <p className="text-gray-900 dark:text-gray-100">{user.role}</p>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

export default function UsersPage() {
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState("");
  const [sortKey, setSortKey] = useState<SortKey>("name");
  const [sortDir, setSortDir] = useState<SortDir>("asc");
  const [page, setPage] = useState(0);
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const perPage = 10;

  const filtered = useMemo(() => {
    const q = search.toLowerCase().trim();
    let list = MOCK_USERS;
    if (q) {
      list = list.filter(
        (u) =>
          u.name.toLowerCase().includes(q) ||
          u.email.toLowerCase().includes(q) ||
          u.mobile.includes(q) ||
          u.id.toLowerCase().includes(q) ||
          u.role.toLowerCase().includes(q) ||
          u.status.toLowerCase().includes(q)
      );
    }
    list.sort((a, b) => {
      const aVal = (a[sortKey] ?? "").toString().toLowerCase();
      const bVal = (b[sortKey] ?? "").toString().toLowerCase();
      return sortDir === "asc" ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
    });
    return list;
  }, [search, sortKey, sortDir]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / perPage));
  const paged = filtered.slice(page * perPage, (page + 1) * perPage);

  const handleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortKey(key);
      setSortDir("asc");
    }
    setPage(0);
  };

  const refresh = useCallback(() => {
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      toast.success("Users refreshed");
    }, 600);
  }, []);

  const openDetail = (user: AdminUser) => {
    setSelectedUser(user);
    setDialogOpen(true);
  };

  const SortHeader = ({ label, sortKey: sk }: { label: string; sortKey: SortKey }) => (
    <th
      className="cursor-pointer px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400"
      onClick={() => handleSort(sk)}
    >
      <div className="inline-flex items-center gap-1">
        {label}
        <ArrowUpDown className="h-3 w-3" />
      </div>
    </th>
  );

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="space-y-6"
    >
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            User Management
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            Manage all registered users
          </p>
        </div>
        <Button variant="outline" onClick={refresh} disabled={loading}>
          <RefreshCw className={`mr-2 h-4 w-4 ${loading ? "animate-spin" : ""}`} />
          Refresh
        </Button>
      </div>

      <Card>
        <CardContent className="p-4">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <Input
              placeholder="Search by name, email, mobile, ID..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              className="pl-10"
            />
          </div>
        </CardContent>
      </Card>

      {loading ? (
        <Card>
          <CardContent className="p-0">
            <div className="divide-y divide-gray-100 dark:divide-gray-800">
              {Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="flex items-center gap-4 p-4">
                  <Skeleton className="h-8 w-8 rounded-full" />
                  <Skeleton className="h-4 flex-1" />
                  <Skeleton className="h-4 w-24" />
                  <Skeleton className="h-4 w-20" />
                  <Skeleton className="h-4 w-16" />
                  <Skeleton className="h-4 w-28" />
                  <Skeleton className="h-8 w-16" />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      ) : filtered.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-16">
            <Users className="mb-3 h-12 w-12 text-gray-300 dark:text-gray-600" />
            <p className="text-lg font-medium text-gray-500 dark:text-gray-400">
              No users found
            </p>
            <p className="text-sm text-gray-400 dark:text-gray-500">
              Try adjusting your search query
            </p>
          </CardContent>
        </Card>
      ) : (
        <>
          <Card>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-gray-200 dark:border-gray-700">
                      <SortHeader label="Name" sortKey="name" />
                      <SortHeader label="Mobile" sortKey="mobile" />
                      <SortHeader label="Email" sortKey="email" />
                      <SortHeader label="Role" sortKey="role" />
                      <SortHeader label="Status" sortKey="status" />
                      <SortHeader label="Joined Date" sortKey="joinedAt" />
                      <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-400">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                    {paged.map((user) => (
                      <tr
                        key={user.id}
                        className="group cursor-pointer transition-colors hover:bg-gray-50 dark:hover:bg-gray-800/50"
                        onClick={() =>
                          setExpandedId(expandedId === user.id ? null : user.id)
                        }
                      >
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-3">
                            <Avatar className="h-8 w-8">
                              <div className="flex h-full w-full items-center justify-center rounded-full bg-brand-100 text-xs font-semibold text-brand-700 dark:bg-brand-900/30 dark:text-brand-300">
                                {user.name
                                  .split(" ")
                                  .map((n) => n[0])
                                  .join("")
                                  .toUpperCase()
                                  .slice(0, 2)}
                              </div>
                            </Avatar>
                            <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
                              {user.name}
                            </span>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">
                          {maskMobile(user.mobile)}
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">
                          {user.email}
                        </td>
                        <td className="px-4 py-3">
                          <Badge variant={ROLE_VARIANT[user.role]}>
                            {user.role}
                          </Badge>
                        </td>
                        <td className="px-4 py-3">
                          <Badge variant={STATUS_VARIANT[user.status]}>
                            {user.status}
                          </Badge>
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">
                          {formatDate(user.joinedAt)}
                        </td>
                        <td className="px-4 py-3">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={(e) => {
                              e.stopPropagation();
                              openDetail(user);
                            }}
                          >
                            <Eye className="mr-1 h-4 w-4" />
                            View
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
          </Card>

          <div className="flex items-center justify-between">
            <p className="text-sm text-gray-500 dark:text-gray-400">
              Showing {(page * perPage) + 1}–{Math.min((page + 1) * perPage, filtered.length)} of{" "}
              {filtered.length} users
            </p>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage((p) => p - 1)}
              >
                <ChevronLeft className="h-4 w-4" />
                Previous
              </Button>
              <span className="text-sm text-gray-500 dark:text-gray-400">
                Page {page + 1} of {totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </>
      )}

      <UserDetailDialog
        user={selectedUser}
        open={dialogOpen}
        onClose={() => {
          setDialogOpen(false);
          setSelectedUser(null);
        }}
      />
    </motion.div>
  );
}

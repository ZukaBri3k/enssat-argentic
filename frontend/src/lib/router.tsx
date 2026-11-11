import React, { createContext, useCallback, useContext, useEffect, useState } from "react";

interface LocationState {
  pathname: string;
  search: string;
}

interface RouterContextValue {
  location: LocationState;
  navigate: (to: string, options?: { replace?: boolean }) => void;
}

const RouterContext = createContext<RouterContextValue | undefined>(undefined);

function getCurrentLocation(): LocationState {
  return {
    pathname: window.location.pathname || "/",
    search: window.location.search || "",
  };
}

export function BrowserRouter({ children }: { children: React.ReactNode }) {
  const [location, setLocation] = useState<LocationState>(() => getCurrentLocation());

  useEffect(() => {
    const handlePopState = () => {
      setLocation(getCurrentLocation());
    };
    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  const navigate = useCallback((to: string, options?: { replace?: boolean }) => {
    if (options?.replace) {
      window.history.replaceState({}, "", to);
    } else {
      window.history.pushState({}, "", to);
    }
    setLocation(getCurrentLocation());
  }, []);

  return (
    <RouterContext.Provider value={{ location, navigate }}>
      {children}
    </RouterContext.Provider>
  );
}

export function useLocation(): LocationState {
  const ctx = useContext(RouterContext);
  if (!ctx) {
    throw new Error("useLocation doit être utilisé dans un BrowserRouter");
  }
  return ctx.location;
}

export function useNavigate() {
  const ctx = useContext(RouterContext);
  if (!ctx) {
    throw new Error("useNavigate doit être utilisé dans un BrowserRouter");
  }
  return ctx.navigate;
}

export function Navigate({ to, replace = true }: { to: string; replace?: boolean }) {
  const navigate = useNavigate();

  useEffect(() => {
    navigate(to, { replace });
  }, [navigate, to, replace]);

  return null;
}

interface LinkProps extends Omit<React.AnchorHTMLAttributes<HTMLAnchorElement>, "href"> {
  to: string;
  replace?: boolean;
}

export function Link({ to, replace, onClick, children, ...rest }: LinkProps) {
  const navigate = useNavigate();

  const handleClick = (e: React.MouseEvent<HTMLAnchorElement>) => {
    if (onClick) onClick(e);
    if (
      e.defaultPrevented ||
      e.button !== 0 ||
      e.metaKey ||
      e.altKey ||
      e.ctrlKey ||
      e.shiftKey
    ) {
      return;
    }
    e.preventDefault();
    navigate(to, { replace });
  };

  return (
    <a href={to} onClick={handleClick} {...rest}>
      {children}
    </a>
  );
}

interface NavLinkProps extends Omit<LinkProps, "className"> {
  className?: string | ((props: { isActive: boolean }) => string);
  end?: boolean;
}

export function NavLink({ to, end = false, className, children, ...rest }: NavLinkProps) {
  const { pathname } = useLocation();
  const isActive = end
    ? pathname === to
    : pathname === to || pathname.startsWith(`${to}/`);

  const computedClassName =
    typeof className === "function" ? className({ isActive }) : className;

  return (
    <Link to={to} className={computedClassName} aria-current={isActive ? "page" : undefined} {...rest}>
      {children}
    </Link>
  );
}

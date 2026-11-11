import "./App.css";
import { AuthProvider } from "@/context/AuthContext";
import { LoginForm } from "@/components/LoginForm";

function App() {
  return (
    <AuthProvider>
      <div className="min-h-screen w-full flex flex-col justify-between p-4 md:p-8 bg-gradient-to-b from-background via-muted/30 to-background">
        <header className="w-full max-w-5xl mx-auto flex items-center justify-between py-2">
          <div className="flex items-center gap-2 text-foreground font-semibold tracking-tight">
            <span className="w-2.5 h-2.5 rounded-full bg-primary inline-block"></span>
            <span>Studio Argentic</span>
          </div>
          <span className="text-xs text-muted-foreground font-mono">v1.0.0</span>
        </header>

        <main className="w-full flex items-center justify-center py-6">
          <LoginForm />
        </main>

        <footer className="w-full max-w-5xl mx-auto text-center py-4 text-xs text-muted-foreground">
          © {new Date().getFullYear()} Studio Argentic — Plateforme de gestion de studio photographique
        </footer>
      </div>
    </AuthProvider>
  );
}

export default App;

import { Link } from "react-router-dom";
import {
  FileText,
  Shield,
  Users,
  Bell,
  Share2,
  ArrowRight,
  CheckCircle2,
  Star,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Logo } from "@/components/shared/Logo";

export function LandingPage() {
  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <header className="border-b border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <Logo size="sm" />
            <div className="flex items-center gap-4">
              <Link to="/login">
                <Button variant="ghost" className="text-slate-700">
                  Sign in
                </Button>
              </Link>
              <Link to="/register">
                <Button className="bg-primary-600 hover:bg-primary-700 text-white">
                  Get Started
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="bg-gradient-to-b from-slate-50 to-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 md:py-32">
          <div className="text-center max-w-3xl mx-auto">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary-50 text-primary-700 text-sm font-medium mb-6">
              <Star className="h-4 w-4" />
              Professional Document Management
            </div>

            <h1 className="text-4xl md:text-6xl font-bold text-slate-900 tracking-tight mb-6">
              Manage your documents with{" "}
              <span className="text-primary-600">confidence</span>
            </h1>

            <p className="text-lg md:text-xl text-slate-600 mb-8">
              DocuSphere is the secure, professional platform for organizing,
              validating, and sharing your business documents. Built for teams
              that demand reliability and control.
            </p>

            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link to="/register">
                <Button
                  size="lg"
                  className="bg-primary-600 hover:bg-primary-700 text-white px-8"
                >
                  Start Free Trial
                  <ArrowRight className="ml-2 h-4 w-4" />
                </Button>
              </Link>
              <Link to="/login">
                <Button size="lg" variant="outline" className="px-8">
                  Sign In
                </Button>
              </Link>
            </div>

            <p className="text-sm text-slate-500 mt-4">
              No credit card required · Free 14-day trial
            </p>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 md:py-32 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl md:text-4xl font-bold text-slate-900 mb-4">
              Everything you need for document management
            </h2>
            <p className="text-lg text-slate-600 max-w-2xl mx-auto">
              From upload to approval, DocuSphere handles your entire document
              workflow
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            <FeatureCard
              icon={FileText}
              title="Smart Organization"
              description="Hierarchical folders, intelligent search, and automatic categorization keep your documents organized."
            />
            <FeatureCard
              icon={Shield}
              title="Enterprise Security"
              description="Role-based access control, encryption at rest, and comprehensive audit trails protect your data."
            />
            <FeatureCard
              icon={Users}
              title="Team Collaboration"
              description="Share documents with your team, manage permissions, and track every interaction."
            />
            <FeatureCard
              icon={CheckCircle2}
              title="Approval Workflows"
              description="Route documents through multi-step approval processes with automatic notifications."
            />
            <FeatureCard
              icon={Bell}
              title="Real-time Notifications"
              description="Stay informed with instant notifications for document updates, approvals, and shares."
            />
            <FeatureCard
              icon={Share2}
              title="Secure Sharing"
              description="Share documents externally with time-limited links, QR codes, and download tracking."
            />
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-16 bg-slate-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
            <StatItem value="99.9%" label="Uptime SLA" />
            <StatItem value="50GB" label="Storage per user" />
            <StatItem value="256-bit" label="AES Encryption" />
            <StatItem value="24/7" label="Support" />
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section className="py-20 md:py-32 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16">
            <h2 className="text-3xl md:text-4xl font-bold text-slate-900 mb-4">
              How DocuSphere works
            </h2>
            <p className="text-lg text-slate-600 max-w-2xl mx-auto">
              Get started in minutes with our simple three-step process
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <StepCard
              number="1"
              title="Upload"
              description="Drag and drop your documents into organized folders. We support PDF, Word, Excel, images, and more."
            />
            <StepCard
              number="2"
              title="Review & Approve"
              description="Route documents through your approval workflow. Track status and receive notifications at every step."
            />
            <StepCard
              number="3"
              title="Share & Collaborate"
              description="Share approved documents with your team or external partners using secure, time-limited links."
            />
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 bg-primary-600">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
            Ready to transform your document management?
          </h2>
          <p className="text-lg text-primary-100 mb-8">
            Join thousands of teams who trust DocuSphere for their critical
            documents
          </p>
          <Link to="/register">
            <Button
              size="lg"
              variant="secondary"
              className="px-8 bg-white text-primary-600 hover:bg-primary-50"
            >
              Start Your Free Trial
              <ArrowRight className="ml-2 h-4 w-4" />
            </Button>
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-slate-900 text-slate-400 py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex flex-col md:flex-row justify-between items-center gap-6">
            <Logo size="sm" light />
            <div className="flex gap-6 text-sm">
              <a href="#" className="hover:text-white transition-colors">
                Privacy
              </a>
              <a href="#" className="hover:text-white transition-colors">
                Terms
              </a>
              <a href="#" className="hover:text-white transition-colors">
                Security
              </a>
              <a href="#" className="hover:text-white transition-colors">
                Contact
              </a>
            </div>
            <p className="text-sm">© 2026 DocuSphere. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}

// Composants auxiliaires
function FeatureCard({
  icon: Icon,
  title,
  description,
}: Readonly<{
  icon: React.ElementType;
  title: string;
  description: string;
}>) {
  return (
    <div className="p-6 rounded-xl border border-slate-200 hover:border-primary-200 hover:shadow-sm transition-all">
      <div className="h-12 w-12 rounded-lg bg-primary-50 flex items-center justify-center mb-4">
        <Icon className="h-6 w-6 text-primary-600" />
      </div>
      <h3 className="text-lg font-semibold text-slate-900 mb-2">{title}</h3>
      <p className="text-sm text-slate-600 leading-relaxed">{description}</p>
    </div>
  );
}

function StatItem({
  value,
  label,
}: Readonly<{ value: string; label: string }>) {
  return (
    <div>
      <div className="text-3xl md:text-4xl font-bold text-primary-600 mb-2">
        {value}
      </div>
      <div className="text-sm text-slate-600">{label}</div>
    </div>
  );
}

function StepCard({
  number,
  title,
  description,
}: Readonly<{
  number: string;
  title: string;
  description: string;
}>) {
  return (
    <div className="text-center">
      <div className="h-12 w-12 rounded-full bg-primary-600 text-white flex items-center justify-center text-xl font-bold mx-auto mb-4">
        {number}
      </div>
      <h3 className="text-lg font-semibold text-slate-900 mb-2">{title}</h3>
      <p className="text-sm text-slate-600 leading-relaxed">{description}</p>
    </div>
  );
}

interface LogoProps {
  size?: "sm" | "md" | "lg";
  showWordmark?: boolean;
  light?: boolean; // Pour les fonds sombres
}

const sizes = {
  sm: { icon: 24, text: "text-base" },
  md: { icon: 32, text: "text-lg" },
  lg: { icon: 40, text: "text-xl" },
};

export function Logo({
  size = "md",
  showWordmark = true,
  light = false,
}: Readonly<LogoProps>) {
  const { icon, text } = sizes[size];

  return (
    <div className="flex items-center gap-2.5">
      {/* Icon */}
      <div
        className="rounded-lg bg-primary-600 flex items-center justify-center shrink-0"
        style={{ width: icon, height: icon }}
      >
        <svg
          width={icon * 0.6}
          height={icon * 0.6}
          viewBox="0 0 24 24"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
        >
          {/* Document shape */}
          <path
            d="M6 3H14L18 7V19C18 20.1046 17.1046 21 16 21H6C4.89543 21 4 20.1046 4 19V5C4 3.89543 4.89543 3 6 3Z"
            fill="white"
            fillOpacity="0.9"
          />
          {/* Folded corner */}
          <path
            d="M14 3L18 7H15C14.4477 7 14 6.55228 14 6V3Z"
            fill="white"
            fillOpacity="0.6"
          />
          {/* Sphere/orbit element */}
          <circle
            cx="11"
            cy="14"
            r="4"
            stroke="white"
            strokeWidth="1.5"
            fill="none"
            opacity="0.9"
          />
          <ellipse
            cx="11"
            cy="14"
            rx="4"
            ry="1.5"
            stroke="white"
            strokeWidth="1.2"
            fill="none"
            opacity="0.6"
            transform="rotate(-20 11 14)"
          />
        </svg>
      </div>

      {/* Wordmark */}
      {showWordmark && (
        <span
          className={`font-semibold tracking-tight ${text} ${
            light ? "text-white" : "text-slate-900"
          }`}
        >
          Docu<span className="text-primary-600">Sphere</span>
        </span>
      )}
    </div>
  );
}

export function LogoIconOnly({ size = "md" }: Readonly<{ size?: "sm" | "md" | "lg" }>) {
  return <Logo size={size} showWordmark={false} />;
}

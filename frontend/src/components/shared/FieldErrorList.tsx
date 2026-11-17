interface FieldErrorListProps {
  fieldErrors: Record<string, string>;
  // Message déjà affiché au-dessus de la liste (souvent identique à l'erreur de l'unique champ)
  excluding?: string;
  className?: string;
}

export function FieldErrorList({ fieldErrors, excluding, className }: FieldErrorListProps) {
  // Les messages par champ sont déjà rédigés pour l'utilisateur : on n'affiche pas le nom technique du champ
  const messages = Object.entries(fieldErrors).filter(([, msg]) => msg !== excluding);
  if (messages.length === 0) return null;

  return (
    <ul className={className}>
      {messages.map(([field, msg]) => (
        <li key={field}>{msg}</li>
      ))}
    </ul>
  );
}

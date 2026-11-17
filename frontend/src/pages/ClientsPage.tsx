import React, { useEffect, useMemo, useState } from "react";
import {
  apiCreateClient,
  apiDeleteClient,
  apiGetClients,
  apiUpdateClient,
  TYPE_CLIENT_LABELS,
  type Client,
  type ClientRequest,
  type TypeClient,
} from "@/lib/api";
import { useToast } from "@/components/shared/ToastProvider";
import { ConfirmDialog } from "@/components/shared/ConfirmDialog";
import { ErrorAlert } from "@/components/shared/ErrorAlert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { TONS } from "@/lib/tons";
import {
  Building2,
  FileText,
  Loader2,
  Mail,
  MapPin,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Trash2,
  User,
  Users,
} from "lucide-react";

interface FormState {
  type: TypeClient;
  email: string;
  adressePostale: string;
  nom: string;
  prenom: string;
  raisonSociale: string;
  numeroSirenTva: string;
}

const initialFormState: FormState = {
  type: "PARTICULIER",
  email: "",
  adressePostale: "",
  nom: "",
  prenom: "",
  raisonSociale: "",
  numeroSirenTva: "",
};

export function ClientsPage() {
  const { toast } = useToast();

  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [pageError, setPageError] = useState<unknown>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [filterType, setFilterType] = useState<"ALL" | TypeClient>("ALL");

  // Formulaire (création ou édition)
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingClient, setEditingClient] = useState<Client | null>(null);
  const [formData, setFormData] = useState<FormState>(initialFormState);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [formError, setFormError] = useState<unknown>(null);

  // Modale de confirmation de suppression
  const [clientToDelete, setClientToDelete] = useState<Client | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  const fetchClients = async () => {
    setLoading(true);
    setPageError(null);
    try {
      const data = await apiGetClients();
      setClients(data);
    } catch (err) {
      setPageError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchClients();
  }, []);

  const openCreateForm = () => {
    setEditingClient(null);
    setFormData(initialFormState);
    setFormError(null);
    setIsFormOpen(true);
  };

  const openEditForm = (client: Client) => {
    setEditingClient(client);
    setFormData({
      type: client.type,
      email: client.email || "",
      adressePostale: client.adressePostale || "",
      nom: client.nom || "",
      prenom: client.prenom || "",
      raisonSociale: client.raisonSociale || "",
      numeroSirenTva: client.numeroSirenTva || "",
    });
    setFormError(null);
    setIsFormOpen(true);
  };

  const closeForm = () => {
    if (isSubmitting) return;
    setIsFormOpen(false);
    setEditingClient(null);
    setFormData(initialFormState);
    setFormError(null);
  };

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const payload: ClientRequest = {
      type: formData.type,
      email: formData.email.trim(),
      adressePostale: formData.adressePostale.trim(),
    };

    if (formData.type === "PARTICULIER") {
      payload.nom = formData.nom.trim();
      payload.prenom = formData.prenom.trim();
    } else {
      payload.raisonSociale = formData.raisonSociale.trim();
      payload.numeroSirenTva = formData.numeroSirenTva.trim();
    }

    setIsSubmitting(true);
    try {
      if (editingClient) {
        const updated = await apiUpdateClient(editingClient.id, payload);
        setClients((prev) => prev.map((c) => (c.id === updated.id ? updated : c)));
        toast({
          title: "Client modifié",
          description: `Les informations de ${updated.identite} ont été mises à jour.`,
          variant: "success",
        });
      } else {
        const created = await apiCreateClient(payload);
        setClients((prev) => [created, ...prev]);
        toast({
          title: "Client créé",
          description: `Le client ${created.identite} a été ajouté avec succès.`,
          variant: "success",
        });
      }
      setIsFormOpen(false);
      setEditingClient(null);
      setFormData(initialFormState);
    } catch (err) {
      setFormError(err);
    } finally {
      setIsSubmitting(false);
    }
  };

  const confirmDelete = async () => {
    if (!clientToDelete) return;
    setIsDeleting(true);
    try {
      await apiDeleteClient(clientToDelete.id);
      setClients((prev) => prev.filter((c) => c.id !== clientToDelete.id));
      toast({
        title: "Client supprimé",
        description: `Le client ${clientToDelete.identite} a été supprimé du carnet.`,
        variant: "success",
      });
      setClientToDelete(null);
    } catch (err) {
      setPageError(err);
      setClientToDelete(null);
    } finally {
      setIsDeleting(false);
    }
  };

  const filteredClients = useMemo(() => {
    return clients.filter((c) => {
      if (filterType !== "ALL" && c.type !== filterType) {
        return false;
      }
      if (!searchTerm.trim()) {
        return true;
      }
      const term = searchTerm.toLowerCase();
      return (
        c.identite.toLowerCase().includes(term) ||
        c.email.toLowerCase().includes(term) ||
        (c.adressePostale && c.adressePostale.toLowerCase().includes(term)) ||
        (c.numeroSirenTva && c.numeroSirenTva.toLowerCase().includes(term))
      );
    });
  }, [clients, filterType, searchTerm]);

  return (
    <div className="space-y-6">
      {/* En-tête */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <h1 className="text-2xl font-bold tracking-tight text-foreground flex items-center gap-2.5">
            <Users className="w-6 h-6 text-primary shrink-0" />
            <span>Gestion des clients</span>
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Carnet des clients particuliers et entreprises du studio photographique.
          </p>
        </div>

        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:gap-2.5 sm:shrink-0">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={fetchClients}
            disabled={loading}
            className="cursor-pointer gap-1.5 w-full h-9 sm:w-auto sm:h-7"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? "animate-spin" : ""}`} />
            <span>Actualiser</span>
          </Button>

          <Button
            type="button"
            size="sm"
            onClick={openCreateForm}
            className="cursor-pointer gap-1.5 w-full h-9 sm:w-auto sm:h-7"
          >
            <Plus className="w-4 h-4" />
            <span>Nouveau client</span>
          </Button>
        </div>
      </div>

      {/* Erreur de page éventuelle */}
      <ErrorAlert error={pageError} onDismiss={() => setPageError(null)} />

      {/* Modal / Card Formulaire de Création / Édition */}
      {isFormOpen && (
        <Card className="border-primary/40 shadow-md">
          <CardHeader className="pb-3 border-b border-border/60">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div className="min-w-0">
                <CardTitle className="text-lg flex items-center gap-2">
                  {editingClient ? (
                    <>
                      <Pencil className="w-4 h-4 text-primary" />
                      <span>Modifier le client</span>
                    </>
                  ) : (
                    <>
                      <Plus className="w-4 h-4 text-primary" />
                      <span>Ajouter un nouveau client</span>
                    </>
                  )}
                </CardTitle>
                <CardDescription>
                  {editingClient
                    ? `Modification de ${editingClient.identite}. Le type de client ne peut pas être modifié.`
                    : "Renseignez les coordonnées selon qu'il s'agit d'un particulier ou d'une entreprise."}
                </CardDescription>
              </div>

              {editingClient && (
                <span
                  className={`inline-flex items-center gap-1.5 self-start shrink-0 px-2.5 py-1 rounded-full text-xs font-semibold ${
                    editingClient.type === "ENTREPRISE"
                      ? `border ${TONS.accent.badge}`
                      : `border ${TONS.info.badge}`
                  }`}
                >
                  {editingClient.type === "ENTREPRISE" ? (
                    <Building2 className="w-3.5 h-3.5" />
                  ) : (
                    <User className="w-3.5 h-3.5" />
                  )}
                  {TYPE_CLIENT_LABELS[editingClient.type]} (non modifiable)
                </span>
              )}
            </div>
          </CardHeader>

          <form onSubmit={handleFormSubmit}>
            <CardContent className="space-y-4 pt-4">
              <ErrorAlert error={formError} onDismiss={() => setFormError(null)} />

              {/* Sélection du type de client (verrouillé en édition) */}
              {!editingClient && (
                <div className="space-y-1.5">
                  <Label>Type de client *</Label>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <button
                      type="button"
                      onClick={() => setFormData((prev) => ({ ...prev, type: "PARTICULIER" }))}
                      className={`flex items-center gap-2.5 p-3 rounded-lg border text-left cursor-pointer transition-all ${
                        formData.type === "PARTICULIER"
                          ? "border-chart-1 bg-chart-1/5 text-foreground ring-1 ring-chart-1"
                          : "border-border hover:bg-muted/40 text-muted-foreground"
                      }`}
                    >
                      <User className={`w-4 h-4 shrink-0 ${TONS.info.text}`} />
                      <div className="min-w-0">
                        <div className="text-sm font-semibold text-foreground">Particulier</div>
                        <div className="text-xs text-muted-foreground">Nom & Prénom requis</div>
                      </div>
                    </button>

                    <button
                      type="button"
                      onClick={() => setFormData((prev) => ({ ...prev, type: "ENTREPRISE" }))}
                      className={`flex items-center gap-2.5 p-3 rounded-lg border text-left cursor-pointer transition-all ${
                        formData.type === "ENTREPRISE"
                          ? "border-chart-4 bg-chart-4/5 text-foreground ring-1 ring-chart-4"
                          : "border-border hover:bg-muted/40 text-muted-foreground"
                      }`}
                    >
                      <Building2 className={`w-4 h-4 shrink-0 ${TONS.accent.text}`} />
                      <div className="min-w-0">
                        <div className="text-sm font-semibold text-foreground">Entreprise</div>
                        <div className="text-xs text-muted-foreground">Raison sociale & SIREN/TVA</div>
                      </div>
                    </button>
                  </div>
                </div>
              )}

              {/* Champs conditionnels selon le type */}
              {formData.type === "PARTICULIER" ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <Label htmlFor="client-nom">Nom *</Label>
                    <Input
                      id="client-nom"
                      type="text"
                      placeholder="Dupont"
                      value={formData.nom}
                      onChange={(e) => setFormData((prev) => ({ ...prev, nom: e.target.value }))}
                      required
                      disabled={isSubmitting}
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="client-prenom">Prénom *</Label>
                    <Input
                      id="client-prenom"
                      type="text"
                      placeholder="Marie"
                      value={formData.prenom}
                      onChange={(e) => setFormData((prev) => ({ ...prev, prenom: e.target.value }))}
                      required
                      disabled={isSubmitting}
                    />
                  </div>
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div className="space-y-1.5">
                    <Label htmlFor="client-raison-sociale">Raison sociale *</Label>
                    <Input
                      id="client-raison-sociale"
                      type="text"
                      placeholder="Atelier Breizh SARL"
                      value={formData.raisonSociale}
                      onChange={(e) => setFormData((prev) => ({ ...prev, raisonSociale: e.target.value }))}
                      required
                      disabled={isSubmitting}
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="client-siren">Numéro SIREN / TVA *</Label>
                    <Input
                      id="client-siren"
                      type="text"
                      placeholder="FR40123456789"
                      value={formData.numeroSirenTva}
                      onChange={(e) => setFormData((prev) => ({ ...prev, numeroSirenTva: e.target.value }))}
                      required
                      disabled={isSubmitting}
                    />
                  </div>
                </div>
              )}

              {/* Champs communs : Email et Adresse postale */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <Label htmlFor="client-email">Email de contact *</Label>
                  <div className="relative">
                    <Mail className="w-4 h-4 absolute left-3 top-2.5 text-muted-foreground pointer-events-none" />
                    <Input
                      id="client-email"
                      type="email"
                      placeholder="contact@exemple.fr"
                      value={formData.email}
                      onChange={(e) => setFormData((prev) => ({ ...prev, email: e.target.value }))}
                      className="pl-9"
                      required
                      disabled={isSubmitting}
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="client-adresse">Adresse postale complète *</Label>
                  <div className="relative">
                    <MapPin className="w-4 h-4 absolute left-3 top-2.5 text-muted-foreground pointer-events-none" />
                    <Input
                      id="client-adresse"
                      type="text"
                      placeholder="12 rue de Kerampont, 22300 Lannion"
                      value={formData.adressePostale}
                      onChange={(e) => setFormData((prev) => ({ ...prev, adressePostale: e.target.value }))}
                      className="pl-9"
                      required
                      disabled={isSubmitting}
                    />
                  </div>
                </div>
              </div>

              {/* Actions du formulaire */}
              <div className="flex flex-col-reverse gap-2 pt-3 border-t border-border/60 sm:flex-row sm:justify-end sm:gap-2.5">
                <Button
                  type="button"
                  variant="outline"
                  onClick={closeForm}
                  disabled={isSubmitting}
                  className="cursor-pointer w-full h-9 sm:w-auto sm:h-8"
                >
                  Annuler
                </Button>
                <Button type="submit" disabled={isSubmitting} className="cursor-pointer gap-2 w-full h-9 sm:w-auto sm:h-8">
                  {isSubmitting && <Loader2 className="w-4 h-4 animate-spin" />}
                  <span>{editingClient ? "Enregistrer les modifications" : "Créer le client"}</span>
                </Button>
              </div>
            </CardContent>
          </form>
        </Card>
      )}

      {/* Barre de recherche et filtres */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="relative w-full sm:flex-1 sm:max-w-sm">
          <Search className="w-4 h-4 absolute left-3 top-2.5 text-muted-foreground pointer-events-none" />
          <Input
            type="search"
            placeholder="Rechercher par nom, email, ville, SIREN..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9"
          />
        </div>

        <div className="flex w-full items-center gap-1 bg-muted p-1 rounded-lg sm:w-auto">
          <Button
            type="button"
            size="xs"
            variant={filterType === "ALL" ? "default" : "ghost"}
            onClick={() => setFilterType("ALL")}
            className="cursor-pointer flex-auto min-w-0 h-auto min-h-9 py-1 whitespace-normal leading-tight sm:flex-none sm:h-6 sm:min-h-0 sm:py-0 sm:whitespace-nowrap"
          >
            Tous ({clients.length})
          </Button>
          <Button
            type="button"
            size="xs"
            variant={filterType === "PARTICULIER" ? "default" : "ghost"}
            onClick={() => setFilterType("PARTICULIER")}
            className="cursor-pointer gap-1.5 flex-auto min-w-0 h-auto min-h-9 py-1 whitespace-normal leading-tight sm:flex-none sm:h-6 sm:min-h-0 sm:py-0 sm:whitespace-nowrap"
          >
            <User className="hidden w-3.5 h-3.5 sm:block" />
            Particuliers ({clients.filter((c) => c.type === "PARTICULIER").length})
          </Button>
          <Button
            type="button"
            size="xs"
            variant={filterType === "ENTREPRISE" ? "default" : "ghost"}
            onClick={() => setFilterType("ENTREPRISE")}
            className="cursor-pointer gap-1.5 flex-auto min-w-0 h-auto min-h-9 py-1 whitespace-normal leading-tight sm:flex-none sm:h-6 sm:min-h-0 sm:py-0 sm:whitespace-nowrap"
          >
            <Building2 className="hidden w-3.5 h-3.5 sm:block" />
            Entreprises ({clients.filter((c) => c.type === "ENTREPRISE").length})
          </Button>
        </div>
      </div>

      {/* Liste des clients */}
      {loading ? (
        <div className="py-16 flex flex-col items-center justify-center gap-2 text-muted-foreground text-sm">
          <Loader2 className="w-6 h-6 animate-spin text-primary" />
          <span>Chargement du carnet de clients...</span>
        </div>
      ) : filteredClients.length === 0 ? (
        <Card className="border-dashed">
          <CardHeader className="text-center items-center py-12">
            <div className="p-3 rounded-2xl bg-muted text-muted-foreground mb-2">
              <Users className="w-8 h-8" />
            </div>
            <CardTitle>Aucun client trouvé</CardTitle>
            <CardDescription className="max-w-md">
              {searchTerm || filterType !== "ALL"
                ? "Aucun résultat ne correspond à vos critères de recherche ou de filtre."
                : "Votre carnet de clients est actuellement vide. Cliquez sur « Nouveau client » pour en enregistrer un."}
            </CardDescription>
            {!(searchTerm || filterType !== "ALL") && (
              <Button
                type="button"
                size="sm"
                onClick={openCreateForm}
                className="mt-4 cursor-pointer gap-1.5"
              >
                <Plus className="w-4 h-4" />
                <span>Nouveau client</span>
              </Button>
            )}
          </CardHeader>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredClients.map((client) => {
            const isEntreprise = client.type === "ENTREPRISE";

            return (
              <Card
                key={client.id}
                className="flex flex-col justify-between hover:border-primary/40 transition-colors shadow-xs"
              >
                <CardHeader className="pb-3">
                  <div className="flex items-start justify-between gap-3">
                    <div className="space-y-1 min-w-0">
                      <div className="flex items-center gap-2">
                        <span
                          className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold ${
                            isEntreprise
                              ? `border ${TONS.accent.badge}`
                              : `border ${TONS.info.badge}`
                          }`}
                        >
                          {isEntreprise ? (
                            <Building2 className="w-3 h-3" />
                          ) : (
                            <User className="w-3 h-3" />
                          )}
                          {TYPE_CLIENT_LABELS[client.type]}
                        </span>
                      </div>

                      <CardTitle className="text-base font-semibold truncate pt-1">
                        {client.identite}
                      </CardTitle>
                    </div>

                    <div className="flex items-center gap-1 shrink-0">
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-9 sm:size-8 text-muted-foreground hover:text-foreground cursor-pointer"
                        onClick={() => openEditForm(client)}
                        title="Modifier ce client"
                      >
                        <Pencil className="w-3.5 h-3.5" />
                      </Button>
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="size-9 sm:size-8 text-destructive/70 hover:text-destructive hover:bg-destructive/10 cursor-pointer"
                        onClick={() => setClientToDelete(client)}
                        title="Supprimer ce client"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </Button>
                    </div>
                  </div>
                </CardHeader>

                <CardContent className="space-y-2 text-xs text-muted-foreground pt-0">
                  <div className="flex items-start gap-2">
                    <Mail className="w-3.5 h-3.5 mt-0.5 shrink-0 text-muted-foreground" />
                    <a
                      href={`mailto:${client.email}`}
                      className="min-w-0 truncate hover:text-primary transition-colors hover:underline"
                    >
                      {client.email}
                    </a>
                  </div>

                  <div className="flex items-start gap-2">
                    <MapPin className="w-3.5 h-3.5 mt-0.5 shrink-0 text-muted-foreground" />
                    <span className="min-w-0 break-words line-clamp-2">{client.adressePostale}</span>
                  </div>

                  {isEntreprise && client.numeroSirenTva && (
                    <div className="flex items-center gap-2 pt-1 font-mono text-[11px] text-foreground">
                      <FileText className="w-3.5 h-3.5 shrink-0 text-muted-foreground" />
                      <span className="min-w-0 break-all">TVA/SIREN : {client.numeroSirenTva}</span>
                    </div>
                  )}
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}

      {/* Dialogue de confirmation de suppression */}
      <ConfirmDialog
        open={Boolean(clientToDelete)}
        onOpenChange={(open) => {
          if (!open && !isDeleting) setClientToDelete(null);
        }}
        title="Supprimer ce client ?"
        description={
          clientToDelete
            ? `Êtes-vous sûr de vouloir supprimer définitivement le client « ${clientToDelete.identite} » ? Cette action est irréversible.`
            : ""
        }
        confirmLabel="Supprimer définitivement"
        cancelLabel="Annuler"
        variant="destructive"
        isLoading={isDeleting}
        onConfirm={confirmDelete}
      />
    </div>
  );
}

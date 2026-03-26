export type ScooterStatus =
  | 'AVAILABLE'
  | 'RESERVED'
  | 'IN_USE'
  | 'LOCKED'
  | 'MAINTENANCE'
  | 'DISABLED';

export interface Scooter {
  id: number;
  publicCode: string;
  model: string;
  batteryPercentage: number;
  latitude: number | null;
  longitude: number | null;
  address: string | null;
  status: ScooterStatus;
  kilometersTraveled: number | null;
  maintenanceNotes: string | null;
  lastActivityAt: string | null;
  distanceKm: number | null;
  unlockable: boolean;
  unlockBlockedReason: string | null;
}

export interface ScooterFormPayload {
  publicCode: string;
  model: string;
  batteryPercentage: number;
  latitude: number | null;
  longitude: number | null;
  address: string | null;
  kilometersTraveled: number | null;
  maintenanceNotes: string | null;
}

export interface ScooterUpdatePayload {
  model: string;
  batteryPercentage: number;
  latitude: number | null;
  longitude: number | null;
  address: string | null;
  kilometersTraveled: number | null;
  maintenanceNotes: string | null;
}

export interface ScooterFilters {
  query?: string;
  status?: ScooterStatus | null;
  minBattery?: number | null;
  latitude?: number | null;
  longitude?: number | null;
  radiusKm?: number | null;
  page?: number | null;
  size?: number | null;
}

export interface ScooterPageResponse {
  content: Scooter[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

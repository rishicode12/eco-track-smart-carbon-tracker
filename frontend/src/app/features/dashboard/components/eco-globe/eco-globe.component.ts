import {
  Component,
  ElementRef,
  OnInit,
  AfterViewInit,
  OnDestroy,
  ViewChild,
  HostListener,
  NgZone
} from '@angular/core';
import { CommonModule } from '@angular/common';
import * as THREE from 'three';

interface EcoLocation {
  name: string;
  lat: number;
  lon: number;
}

@Component({
  selector: 'app-eco-globe',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './eco-globe.component.html',
  styleUrls: ['./eco-globe.component.scss']
})
export class EcoGlobeComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('globeContainer', { static: true }) globeContainer!: ElementRef<HTMLDivElement>;

  private scene!: THREE.Scene;
  private camera!: THREE.PerspectiveCamera;
  private renderer!: THREE.WebGLRenderer;
  private globeGroup!: THREE.Group;
  private earthMesh!: THREE.Mesh;
  private haloMesh!: THREE.Mesh;
  private atmosphereGlowMesh!: THREE.Mesh;
  private particlesMesh!: THREE.Points;
  private animationFrameId: number | null = null;
  private resizeObserver?: ResizeObserver;

  // Interaction State
  private isDragging = false;
  private previousMousePosition = { x: 0, y: 0 };
  private targetRotationX = 0.2;
  private targetRotationY = 0;
  private currentRotationX = 0.2;
  private currentRotationY = 0;
  private idleTimer: any;
  private isIdle = true;

  public activeNodesCount = 142;
  public globalOffsetKg = 48290;

  // Major Global Sustainability Hubs (Geographically verified coordinates)
  private readonly ecoHubs: EcoLocation[] = [
    { name: 'London', lat: 51.5074, lon: -0.1278 },
    { name: 'New York', lat: 40.7128, lon: -74.006 },
    { name: 'San Francisco', lat: 37.7749, lon: -122.4194 },
    { name: 'Tokyo', lat: 35.6762, lon: 139.6503 },
    { name: 'Sydney', lat: -33.8688, lon: 151.2093 },
    { name: 'São Paulo', lat: -23.5505, lon: -46.6333 },
    { name: 'Singapore', lat: 1.3521, lon: 103.8198 },
    { name: 'New Delhi', lat: 28.6139, lon: 77.209 },
    { name: 'Nairobi', lat: -1.2921, lon: 36.8219 },
    { name: 'Berlin', lat: 52.52, lon: 13.405 },
    { name: 'Cairo', lat: 30.0444, lon: 31.2357 }
  ];

  constructor(private ngZone: NgZone) {}

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    this.initThreeJS();
  }

  ngOnDestroy(): void {
    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
    }
    if (this.idleTimer) {
      clearTimeout(this.idleTimer);
    }
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
    }
    if (this.renderer) {
      this.renderer.dispose();
    }
  }

  private initThreeJS(): void {
    const container = this.globeContainer.nativeElement;
    const width = container.clientWidth || 300;
    const height = container.clientHeight || 300;

    // 1. Scene
    this.scene = new THREE.Scene();

    // 2. Camera
    this.camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000);
    this.camera.position.z = 5.8;

    // 3. Renderer with antialiasing and transparency
    this.renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    this.renderer.setSize(width, height);
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    this.renderer.toneMapping = THREE.ACESFilmicToneMapping;
    this.renderer.toneMappingExposure = 1.1;
    container.appendChild(this.renderer.domElement);

    // 4. Main Group for the Globe assembly
    this.globeGroup = new THREE.Group();
    this.globeGroup.rotation.x = 0.2; // Slight tilt
    this.scene.add(this.globeGroup);

    // 5. Geographically Accurate, Borderless Physical Earth Sphere (NASA Blue Marble Topographical Map)
    const sphereRadius = 2.0;
    const earthGeometry = new THREE.SphereGeometry(sphereRadius, 64, 64);

    // Texture loader with fallback to ensure instant loading
    const textureLoader = new THREE.TextureLoader();
    const earthTexture = textureLoader.load(
      '/earth-blue-marble.jpg',
      (texture) => {
        texture.colorSpace = THREE.SRGBColorSpace;
        texture.wrapS = THREE.RepeatWrapping;
        texture.wrapT = THREE.ClampToEdgeWrapping;
        texture.needsUpdate = true;
      },
      undefined,
      (err) => {
        console.warn('Local texture load error, using high-res CDN fallback:', err);
        // Fallback to high-res CDN if local file path is intercepted
        textureLoader.load('https://raw.githubusercontent.com/mrdoob/three.js/master/examples/textures/planets/earth_atmos_2048.jpg', (fallbackTex) => {
          fallbackTex.colorSpace = THREE.SRGBColorSpace;
          earthMaterial.map = fallbackTex;
          earthMaterial.needsUpdate = true;
        });
      }
    );

    // Standard eco-themed physical material with specular green highlight & emissive eco tone
    const earthMaterial = new THREE.MeshPhongMaterial({
      map: earthTexture,
      shininess: 18,
      specular: new THREE.Color('#10b981'),
      emissive: new THREE.Color('#03281c'),
      emissiveIntensity: 0.35,
      bumpScale: 0.02
    });

    this.earthMesh = new THREE.Mesh(earthGeometry, earthMaterial);
    this.globeGroup.add(this.earthMesh);

    // 6. Subtle Latitude/Longitude Physical Navigation Coordinate Mesh (No Political Borders)
    const gridGeometry = new THREE.SphereGeometry(sphereRadius + 0.008, 36, 18);
    const gridMaterial = new THREE.MeshBasicMaterial({
      color: 0x10b981,
      wireframe: true,
      transparent: true,
      opacity: 0.06
    });
    const gridMesh = new THREE.Mesh(gridGeometry, gridMaterial);
    this.globeGroup.add(gridMesh);

    // 7. Outer Atmospheric Glowing Eco Halo (Fresnel Shader Shell)
    const haloGeometry = new THREE.SphereGeometry(sphereRadius * 1.15, 64, 64);
    const haloShaderMaterial = new THREE.ShaderMaterial({
      vertexShader: `
        varying vec3 vNormal;
        void main() {
          vNormal = normalize(normalMatrix * normal);
          gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
      `,
      fragmentShader: `
        varying vec3 vNormal;
        void main() {
          // Fresnel rim lighting for glowing emerald atmosphere
          float intensity = pow(0.72 - dot(vNormal, vec3(0.0, 0.0, 1.0)), 2.4);
          gl_FragColor = vec4(0.062, 0.725, 0.505, 1.0) * intensity * 1.3;
        }
      `,
      blending: THREE.AdditiveBlending,
      side: THREE.BackSide,
      transparent: true,
      depthWrite: false
    });

    this.haloMesh = new THREE.Mesh(haloGeometry, haloShaderMaterial);
    this.globeGroup.add(this.haloMesh);

    // 8. Soft Inner Green Atmosphere Layer
    const innerAtmoGeometry = new THREE.SphereGeometry(sphereRadius * 1.015, 64, 64);
    const innerAtmoMaterial = new THREE.ShaderMaterial({
      vertexShader: `
        varying vec3 vNormal;
        void main() {
          vNormal = normalize(normalMatrix * normal);
          gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
      `,
      fragmentShader: `
        varying vec3 vNormal;
        void main() {
          float intensity = pow(1.0 - dot(vNormal, vec3(0.0, 0.0, 1.0)), 3.0);
          gl_FragColor = vec4(0.2, 0.9, 0.6, 0.35) * intensity;
        }
      `,
      blending: THREE.AdditiveBlending,
      side: THREE.FrontSide,
      transparent: true,
      depthWrite: false
    });
    this.atmosphereGlowMesh = new THREE.Mesh(innerAtmoGeometry, innerAtmoMaterial);
    this.globeGroup.add(this.atmosphereGlowMesh);

    // 9. Global Sustainability Hub Nodes & Network Arcs
    this.addEcoHubsAndArcs(sphereRadius);

    // 10. Ambient Particle Dust / Stars in Background
    this.createEcoParticles();

    // 11. Lighting Setup
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.9);
    this.scene.add(ambientLight);

    const mainSunLight = new THREE.DirectionalLight(0x34d399, 2.4);
    mainSunLight.position.set(6, 4, 5);
    this.scene.add(mainSunLight);

    const fillEcoLight = new THREE.DirectionalLight(0x059669, 1.3);
    fillEcoLight.position.set(-6, -3, -4);
    this.scene.add(fillEcoLight);

    // Observe element resize dynamically
    if (typeof ResizeObserver !== 'undefined' && container) {
      this.resizeObserver = new ResizeObserver(() => {
        this.onWindowResize();
      });
      this.resizeObserver.observe(container);
    }

    // Start 60 FPS animation loop outside Angular zone
    this.ngZone.runOutsideAngular(() => {
      this.animate();
    });
  }

  /**
   * Convert latitude & longitude into precise 3D Cartesian coordinates on the sphere
   */
  private latLongToVector3(lat: number, lon: number, radius: number): THREE.Vector3 {
    const phi = (90 - lat) * (Math.PI / 180);
    const theta = (lon + 180) * (Math.PI / 180);

    return new THREE.Vector3(
      -(radius * Math.sin(phi) * Math.cos(theta)),
      radius * Math.cos(phi),
      radius * Math.sin(phi) * Math.sin(theta)
    );
  }

  /**
   * Add interactive pulsating nodes and Great Circle network arcs between hubs
   */
  private addEcoHubsAndArcs(radius: number): void {
    const hubPoints: THREE.Vector3[] = [];

    // Render Hub Nodes
    this.ecoHubs.forEach((hub) => {
      const pos = this.latLongToVector3(hub.lat, hub.lon, radius + 0.02);
      hubPoints.push(pos);

      // Inner glowing core
      const coreGeom = new THREE.SphereGeometry(0.032, 16, 16);
      const coreMat = new THREE.MeshBasicMaterial({ color: 0x6ee7b7 });
      const coreMesh = new THREE.Mesh(coreGeom, coreMat);
      coreMesh.position.copy(pos);
      this.globeGroup.add(coreMesh);

      // Outer beacon ring
      const ringGeom = new THREE.RingGeometry(0.045, 0.065, 24);
      const ringMat = new THREE.MeshBasicMaterial({
        color: 0x10b981,
        side: THREE.DoubleSide,
        transparent: true,
        opacity: 0.8
      });
      const ringMesh = new THREE.Mesh(ringGeom, ringMat);
      ringMesh.position.copy(pos);
      ringMesh.lookAt(pos.clone().multiplyScalar(2)); // Align ring tangent to sphere surface
      this.globeGroup.add(ringMesh);
    });

    // Create curved Great Circle Network Arcs between adjacent hubs
    const connections: [number, number][] = [
      [0, 1],  // London - New York
      [1, 2],  // New York - San Francisco
      [2, 3],  // San Francisco - Tokyo
      [3, 6],  // Tokyo - Singapore
      [6, 7],  // Singapore - New Delhi
      [7, 0],  // New Delhi - London
      [0, 9],  // London - Berlin
      [9, 10], // Berlin - Cairo
      [10, 8], // Cairo - Nairobi
      [8, 4],  // Nairobi - Sydney
      [4, 6],  // Sydney - Singapore
      [1, 5],  // New York - São Paulo
      [5, 8]   // São Paulo - Nairobi
    ];

    connections.forEach(([i1, i2]) => {
      const p1 = hubPoints[i1];
      const p2 = hubPoints[i2];
      if (!p1 || !p2) return;

      const distance = p1.distanceTo(p2);
      const midPoint = p1.clone().add(p2).multiplyScalar(0.5);
      const altitude = radius + Math.min(distance * 0.28, 0.6);
      midPoint.normalize().multiplyScalar(altitude);

      const curve = new THREE.QuadraticBezierCurve3(p1, midPoint, p2);
      const curvePoints = curve.getPoints(36);
      const arcGeometry = new THREE.BufferGeometry().setFromPoints(curvePoints);

      const arcMaterial = new THREE.LineBasicMaterial({
        color: 0x34d399,
        transparent: true,
        opacity: 0.4,
        linewidth: 1.5
      });

      const arcLine = new THREE.Line(arcGeometry, arcMaterial);
      this.globeGroup.add(arcLine);
    });
  }

  private createEcoParticles(): void {
    const particleCount = 180;
    const geometry = new THREE.BufferGeometry();
    const positions = new Float32Array(particleCount * 3);

    for (let i = 0; i < particleCount * 3; i += 3) {
      const r = 3.2 + Math.random() * 2.5;
      const theta = Math.random() * Math.PI * 2;
      const phi = Math.acos(Math.random() * 2 - 1);

      positions[i] = r * Math.sin(phi) * Math.cos(theta);
      positions[i + 1] = r * Math.sin(phi) * Math.sin(theta);
      positions[i + 2] = r * Math.cos(phi);
    }

    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));

    const material = new THREE.PointsMaterial({
      color: 0x34d399,
      size: 0.04,
      transparent: true,
      opacity: 0.55,
      blending: THREE.AdditiveBlending
    });

    this.particlesMesh = new THREE.Points(geometry, material);
    this.scene.add(this.particlesMesh);
  }

  private animate(): void {
    this.animationFrameId = requestAnimationFrame(() => this.animate());

    // Auto-rotate slowly when idle
    if (this.isIdle) {
      this.targetRotationY += 0.0025;
    }

    // Smooth inertia lerp for mouse/touch drag
    this.currentRotationX += (this.targetRotationX - this.currentRotationX) * 0.08;
    this.currentRotationY += (this.targetRotationY - this.currentRotationY) * 0.08;

    this.globeGroup.rotation.x = this.currentRotationX;
    this.globeGroup.rotation.y = this.currentRotationY;

    // Slow ambient rotation for particles
    if (this.particlesMesh) {
      this.particlesMesh.rotation.y -= 0.0006;
    }

    this.renderer.render(this.scene, this.camera);
  }

  // Mouse & Touch Drag Interaction Handlers
  public onMouseDown(event: MouseEvent): void {
    this.isDragging = true;
    this.isIdle = false;
    this.previousMousePosition = { x: event.clientX, y: event.clientY };
    this.resetIdleTimer();
  }

  @HostListener('window:mousemove', ['$event'])
  public onMouseMove(event: MouseEvent): void {
    if (!this.isDragging) return;

    const deltaX = event.clientX - this.previousMousePosition.x;
    const deltaY = event.clientY - this.previousMousePosition.y;

    this.targetRotationY += deltaX * 0.007;
    this.targetRotationX += deltaY * 0.007;

    // Clamp vertical rotation to prevent disorienting flips
    this.targetRotationX = Math.max(-Math.PI / 2.6, Math.min(Math.PI / 2.6, this.targetRotationX));

    this.previousMousePosition = { x: event.clientX, y: event.clientY };
    this.resetIdleTimer();
  }

  @HostListener('window:mouseup')
  public onMouseUp(): void {
    this.isDragging = false;
    this.resetIdleTimer();
  }

  public onTouchStart(event: TouchEvent): void {
    if (event.touches.length === 1) {
      this.isDragging = true;
      this.isIdle = false;
      this.previousMousePosition = {
        x: event.touches[0].clientX,
        y: event.touches[0].clientY
      };
      this.resetIdleTimer();
    }
  }

  @HostListener('window:touchmove', ['$event'])
  public onTouchMove(event: TouchEvent): void {
    if (!this.isDragging || event.touches.length !== 1) return;

    const deltaX = event.touches[0].clientX - this.previousMousePosition.x;
    const deltaY = event.touches[0].clientY - this.previousMousePosition.y;

    this.targetRotationY += deltaX * 0.007;
    this.targetRotationX += deltaY * 0.007;

    this.targetRotationX = Math.max(-Math.PI / 2.6, Math.min(Math.PI / 2.6, this.targetRotationX));

    this.previousMousePosition = {
      x: event.touches[0].clientX,
      y: event.touches[0].clientY
    };
    this.resetIdleTimer();
  }

  @HostListener('window:touchend')
  public onTouchEnd(): void {
    this.isDragging = false;
    this.resetIdleTimer();
  }

  private resetIdleTimer(): void {
    if (this.idleTimer) clearTimeout(this.idleTimer);
    this.idleTimer = setTimeout(() => {
      this.isIdle = true;
    }, 2800);
  }

  @HostListener('window:resize')
  public onWindowResize(): void {
    if (!this.globeContainer || !this.renderer || !this.camera) return;
    const container = this.globeContainer.nativeElement;
    const width = container.clientWidth;
    const height = container.clientHeight;

    this.camera.aspect = width / height;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(width, height);
  }
}

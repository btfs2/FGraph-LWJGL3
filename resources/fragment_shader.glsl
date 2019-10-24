#version 330

uniform vec2 resolution;
uniform float currentTime;
uniform vec3 camPos;
uniform vec3 camDir;
uniform vec3 camUp;
uniform sampler2D tex;
uniform bool showStepDepth;

in vec3 pos;

out vec3 color;

#define PI 3.1415926535897932384626433832795
#define RENDER_DEPTH 100
#define CLOSE_ENOUGH 0.001

#define BACKGROUND -1
#define BALL 0
#define BASE 1

#define GRADIENT(pt, func) vec3( \
    func(vec3(pt.x + 0.0001, pt.y, pt.z)) - func(vec3(pt.x - 0.0001, pt.y, pt.z)), \
    func(vec3(pt.x, pt.y + 0.0001, pt.z)) - func(vec3(pt.x, pt.y - 0.0001, pt.z)), \
    func(vec3(pt.x, pt.y, pt.z + 0.0001)) - func(vec3(pt.x, pt.y, pt.z - 0.0001)))

const vec3 LIGHT_POS[] = vec3[](vec3(5, 18, 10));

///////////////////////////////////////////////////////////////////////////////

vec3 getBackground(vec3 dir) {
  float u = 0.5 + atan(dir.z, -dir.x) / (2 * PI);
  float v = 0.5 - asin(dir.y) / PI;
  vec4 texColor = texture(tex, vec2(u, v));
  return texColor.rgb;
}

vec3 getRayDir() {
  vec3 xAxis = normalize(cross(camDir, camUp));
  return normalize(pos.x * (resolution.x / resolution.y) * xAxis + pos.y * camUp + 5 * camDir);
}

float smin(float a, float b, float k) {
	float h = clamp(0.5 + 0.5 * (b - a) / k, 0, 1);
	return mix(b, a, h) - k * h * (1 - h);
}

///////////////////////////////////////////////////////////////////////////////

float sphere(vec3 pt, float r) {
  return length(pt) - r;
}

float sphere(vec3 pt) {
	return sphere(pt, 1.0);
}

float cube(vec3 pt, float r) {  
  vec3 d = abs(pt) - vec3(r);
  return min(max(d.x, max(d.y, d.z)), 0.0) + length(max(d, 0.0));
}

float plane(vec3 pt, vec3 a, vec3 n) {
	return dot((pt-a), n);
}

float torus(vec3 p, vec2 t) {
	vec2 q = vec2(
			length(p.xz) - t.x, p.y);
	return length(q) - t.y;
}

///////////////////////////////////////////////////////////////////////////////

vec3 getNormal(vec3 pt) {
  return normalize(GRADIENT(pt, sphere));
}

vec3 getColor(vec3 pt) {
  return vec3(1);
}

///////////////////////////////////////////////////////////////////////////////

float shade(vec3 eye, vec3 pt, vec3 n) {
  float val = 0;
  
  val += 0.1;  // Ambient
  
  for (int i = 0; i < LIGHT_POS.length(); i++) {
    vec3 l = normalize(LIGHT_POS[i] - pt); 
    val += max(dot(n, l), 0);
  }
  return val;
}

///////////////////////////////////////////////////////////////////////////////

float sdfFloor(vec3 pt) {
	return plane(pt, vec3(0, -1, 0), vec3(0,1,0));
}

float sdfOther(vec3 pt) {
	float val = 99999999999999.9; // Basicly infinity; no inf constnat
	//Union
	float tmp = cube(pt-vec3(-3,0,-3), 1);
	tmp = min(tmp, torus((pt-vec3(-3,1,-3)).xzy, vec2(1, 0.5)));	
	val = min(val, tmp);
	if (val <= CLOSE_ENOUGH) return val;
	//difference
	tmp = cube(pt-vec3(3,0,-3), 1);
	tmp = max(tmp, -torus((pt-vec3(3,1,-3)).xzy, vec2(1, 0.5)));	
	val = min(val, tmp);
	if (val <= CLOSE_ENOUGH) return val;
	//intersection
	tmp = cube(pt-vec3(3,0,3), 1);
	tmp = max(tmp, torus((pt-vec3(3,1,3)).xzy, vec2(1, 0.5)));	
	val = min(val, tmp);
	if (val <= CLOSE_ENOUGH) return val;
	//WILL IT BLEND (no it's running on software emulated GPU you moron, it will barely run at all)
	tmp = cube(pt-vec3(-3,0,3), 1);
	tmp = smin(tmp, torus((pt-vec3(-3,1,3)).xzy, vec2(1, 0.5)), 0.4);	
	val = min(val, tmp);
	if (val <= CLOSE_ENOUGH) return val;
	return val;
}

float sdf(vec3 pt) {
	float val = 99999999999999.9; // Basicly infinity; no inf constnat
	// Main stuff
	val = min(val, sdfOther(pt));
	//Floor
	val = min(val, sdfFloor(pt));
	return val;
}

vec3 illuminate(vec3 camPos, vec3 rayDir, vec3 pt) {
  vec3 c, n;
  n = normalize(GRADIENT(pt, sdf));
  if (sdfFloor(pt) <= CLOSE_ENOUGH) {
 	 c = mix(vec3(0.4, 0.4, 0.4), vec3(0.6, 0.6, 1), mod(sdfOther(pt), 0.5)*2);
  } else  {
 	 c = getColor(pt);
  }
  return shade(camPos, pt, n) * c;
}

vec3 raymarch(vec3 camPos, vec3 rayDir) {
  int step = 0;
  float t = 0;

  for (float d = 1000; step < RENDER_DEPTH && abs(d) > CLOSE_ENOUGH; t += abs(d)) {
    d = sdf(camPos + t * rayDir);
    step++;
  }

  if (step == RENDER_DEPTH) {
    return getBackground(rayDir);
  } else if (showStepDepth) {
    return vec3(float(step) / RENDER_DEPTH);
  } else {
    return illuminate(camPos, rayDir, camPos + t * rayDir);
  }
}

///////////////////////////////////////////////////////////////////////////////

#define thing 1 // My GL driver gives OpenGL failed with error code '1282' when reloading, reloading twice fixes it, so added another toggle flag

void main() {
  color = raymarch(camPos, getRayDir());
}

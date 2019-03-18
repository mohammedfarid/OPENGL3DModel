package com.farid.opengl3dmodel.view;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.farid.opengl3dmodel.controller.TouchController;

/**
 * This is the actual opengl view. From here we can detect touch gestures for example
 *
 * @author andresoviedo
 */
public class ModelSurfaceView extends GLSurfaceView {

    private ModelActivity parent;
    private ModelRenderer mRenderer;
    private TouchController touchHandler;

    public ModelSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(ModelActivity parent) {
        // parent component
        this.parent = parent;
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        // This is the actual renderer of the 3D space
        mRenderer = new ModelRenderer(this);

        // added for transparency
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        //getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);

        setRenderer(mRenderer);

        //getHolder().setFormat(PixelFormat.RGBA_8888);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Render the view only when there is a change in the drawing data
        // TODO: enable this again
        // setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        touchHandler = new TouchController(this, mRenderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return touchHandler.onTouchEvent(event);
    }

    public ModelActivity getModelActivity() {
        return parent;
    }

    public ModelRenderer getModelRenderer() {
        return mRenderer;
    }

}
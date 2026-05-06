const express = require('express');
const multer = require('multer');
const { uploadImage } = require('../../lib/cloudinary');
const { authMiddleware } = require('../../lib/auth');

const router = express.Router();

// Configure multer for memory storage
const upload = multer({
    storage: multer.memoryStorage(),
    limits: {
        fileSize: 10 * 1024 * 1024 // 10MB limit
    },
    fileFilter: (req, file, cb) => {
        if (file.mimetype.startsWith('image/') || file.mimetype === 'application/pdf') {
            cb(null, true);
        } else {
            cb(new Error('Only image and PDF files are allowed'), false);
        }
    }
});

/**
 * POST /api/upload/image
 * Upload an image or PDF file to Cloudinary
 */
router.post('/', authMiddleware, upload.single('image'), async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'NO_FILE',
                    message: 'No file provided'
                }
            });
        }

        const resourceType = req.file.mimetype === 'application/pdf' ? 'raw' : 'image';
        const secureUrl = await uploadImage(req.file.buffer, req.file.mimetype, resourceType);

        res.json({
            success: true,
            data: {
                url: secureUrl,
                filename: req.file.originalname
            }
        });

    } catch (error) {
        console.error('Upload error:', error);
        console.error('Upload error stack:', error.stack);
        res.status(500).json({
            success: false,
            error: {
                code: 'UPLOAD_FAILED',
                message: error.message || 'Failed to upload file',
                details: error.toString()
            }
        });
    }
});

// Error handling middleware for multer
router.use((err, req, res, next) => {
    if (err instanceof multer.MulterError) {
        if (err.code === 'LIMIT_FILE_SIZE') {
            return res.status(400).json({
                success: false,
                error: {
                    code: 'FILE_TOO_LARGE',
                    message: 'File size exceeds 10MB limit'
                }
            });
        }
    }

    if (err.message === 'Only image and PDF files are allowed') {
        return res.status(400).json({
            success: false,
            error: {
                code: 'INVALID_FILE_TYPE',
                message: 'Only image and PDF files are allowed'
            }
        });
    }

    next(err);
});

module.exports = router;